package com.pingplace.location

import android.location.Location
import com.pingplace.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

class OpenStreetMapSearchProvider(
    private val client: OkHttpClient = OkHttpClient(),
    private val interpreterUrl: String = OVERPASS_INTERPRETER_URL
) : NearbyPlaceSearchProvider {

    override suspend fun searchNearby(
        query: String,
        currentLocation: Location,
        radiusMeters: Double
    ): Result<List<NearbyPlace>> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedQuery = normalizeSearchText(query)
            if (normalizedQuery.isBlank()) {
                return@runCatching emptyList()
            }

            val request = Request.Builder()
                .url(interpreterUrl)
                .addHeader("Content-Type", "text/plain")
                .addHeader("User-Agent", "${BuildConfig.APPLICATION_ID}/osm-live-lookup")
                .post(
                    buildOverpassQuery(
                        normalizedQuery = normalizedQuery,
                        currentLocation = currentLocation,
                        radiusMeters = radiusMeters
                    ).toRequestBody("text/plain".toMediaType())
                )
                .build()

            client.newCall(request).execute().use { response ->
                when {
                    response.code == 429 -> throw RateLimitedException()
                    response.code == 504 -> throw ServiceBusyException()
                    !response.isSuccessful -> throw RequestFailedException(response.code)
                }

                val payload = response.body?.string().orEmpty()
                parsePlaces(
                    payload = payload,
                    normalizedQuery = normalizedQuery,
                    currentLocation = currentLocation,
                    radiusMeters = radiusMeters
                )
            }
        }
    }

    private fun buildOverpassQuery(
        normalizedQuery: String,
        currentLocation: Location,
        radiusMeters: Double
    ): String {
        val radius = radiusMeters.roundToInt().coerceIn(500, 10_000)
        val queryPattern = normalizedQuery.split(' ')
            .filter { it.isNotBlank() }
            .joinToString(".*")
        val latitude = String.format(Locale.US, "%.6f", currentLocation.latitude)
        val longitude = String.format(Locale.US, "%.6f", currentLocation.longitude)
        return """
            [out:json][timeout:25];
            (
              nwr(around:$radius,$latitude,$longitude)["name"~"$queryPattern",i];
              nwr(around:$radius,$latitude,$longitude)["brand"~"$queryPattern",i];
              nwr(around:$radius,$latitude,$longitude)["official_name"~"$queryPattern",i];
              nwr(around:$radius,$latitude,$longitude)["operator"~"$queryPattern",i];
              nwr(around:$radius,$latitude,$longitude)["shop"~"$queryPattern",i];
              nwr(around:$radius,$latitude,$longitude)["amenity"~"$queryPattern",i];
            );
            out center tags;
        """.trimIndent()
    }

    private fun parsePlaces(
        payload: String,
        normalizedQuery: String,
        currentLocation: Location,
        radiusMeters: Double
    ): List<NearbyPlace> {
        val root = JSONObject(payload)
        val elements = root.optJSONArray("elements") ?: JSONArray()
        val seen = linkedSetOf<String>()
        return buildList {
            repeat(elements.length()) { index ->
                val item = elements.getJSONObject(index)
                val tags = item.optJSONObject("tags") ?: return@repeat
                val latitude = when {
                    item.has("lat") -> item.optDouble("lat")
                    item.has("center") -> item.getJSONObject("center").optDouble("lat")
                    else -> Double.NaN
                }
                val longitude = when {
                    item.has("lon") -> item.optDouble("lon")
                    item.has("center") -> item.getJSONObject("center").optDouble("lon")
                    else -> Double.NaN
                }
                if (latitude.isNaN() || longitude.isNaN()) return@repeat

                val id = "osm:${item.optString("type")}:${item.optLong("id")}"
                if (!seen.add(id)) return@repeat

                val name = tags.optString("name").ifBlank {
                    tags.optString("brand").ifBlank {
                        tags.optString("shop").ifBlank { tags.optString("amenity") }
                    }
                }
                if (name.isBlank()) return@repeat

                val brand = tags.optString("brand")
                val address = buildAddress(tags)
                val searchText = normalizeSearchText(
                    listOf(
                        name,
                        brand,
                        tags.optString("official_name"),
                        tags.optString("operator"),
                        tags.optString("shop"),
                        tags.optString("amenity"),
                        address
                    ).joinToString(" ")
                )
                val matchRank = matchRank(
                    normalizedQuery = normalizedQuery,
                    searchText = searchText,
                    name = name,
                    brand = brand
                ) ?: return@repeat

                val placeLocation = Location("osm").apply {
                    this.latitude = latitude
                    this.longitude = longitude
                }
                val distance = currentLocation.distanceTo(placeLocation).toDouble()
                if (distance > radiusMeters) return@repeat

                add(
                    RankedPlace(
                        rank = matchRank,
                        place = NearbyPlace(
                            id = id,
                            name = name,
                            address = address,
                            latitude = latitude,
                            longitude = longitude,
                            distanceMeters = distance,
                            estimatedTravelMinutes = TravelTimeEstimator.estimateMinutes(distance, currentLocation)
                        )
                    )
                )
            }
        }.sortedWith(
            compareBy<RankedPlace>({ it.rank }, { it.place.distanceMeters }, { it.place.name })
        ).map { it.place }
            .take(MAX_RESULTS)
    }

    private fun buildAddress(tags: JSONObject): String {
        return listOf(
            listOf(
                tags.optString("addr:housenumber"),
                tags.optString("addr:street")
            ).filter { it.isNotBlank() }.joinToString(" "),
            tags.optString("addr:city"),
            tags.optString("addr:state")
        ).filter { it.isNotBlank() }.joinToString(", ").ifBlank {
            tags.optString("addr:full")
        }
    }

    private fun matchRank(
        normalizedQuery: String,
        searchText: String,
        name: String,
        brand: String
    ): Int? {
        val normalizedName = normalizeSearchText(name)
        val normalizedBrand = normalizeSearchText(brand)
        val queryTokens = normalizedQuery.split(' ').filter { it.isNotBlank() }

        return when {
            normalizedName == normalizedQuery || normalizedBrand == normalizedQuery -> 0
            normalizedName.startsWith(normalizedQuery) || normalizedBrand.startsWith(normalizedQuery) -> 1
            normalizedName.contains(normalizedQuery) || normalizedBrand.contains(normalizedQuery) -> 2
            searchText.contains(normalizedQuery) -> 3
            queryTokens.isNotEmpty() && queryTokens.all { token -> searchText.contains(token) } -> 4
            else -> null
        }
    }

    private fun normalizeSearchText(text: String): String {
        return text.lowercase(Locale.US)
            .replace('_', ' ')
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    class RateLimitedException :
        IllegalStateException("OpenStreetMap lookup is rate-limited right now.")

    class ServiceBusyException :
        IllegalStateException("OpenStreetMap lookup timed out.")

    class RequestFailedException(val code: Int) :
        IllegalStateException("OpenStreetMap lookup failed: $code")

    private data class RankedPlace(
        val rank: Int,
        val place: NearbyPlace
    )

    private companion object {
        const val MAX_RESULTS = 25
        const val OVERPASS_INTERPRETER_URL = "https://overpass-api.de/api/interpreter"
    }
}
