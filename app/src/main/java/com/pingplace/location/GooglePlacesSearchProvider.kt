package com.pingplace.location

import android.location.Location
import com.pingplace.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class GooglePlacesSearchProvider(
    private val client: OkHttpClient = OkHttpClient()
) : NearbyPlaceSearchProvider {

    override suspend fun searchNearby(
        query: String,
        currentLocation: Location,
        radiusMeters: Double
    ): Result<List<NearbyPlace>> {
        if (BuildConfig.PLACES_API_KEY.isBlank()) {
            return Result.failure(MissingPlacesApiKeyException())
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val bodyJson = JSONObject()
                    .put("textQuery", query)
                    .put("maxResultCount", 10)
                    .put(
                        "locationBias",
                        JSONObject().put(
                            "circle",
                            JSONObject()
                                .put(
                                    "center",
                                    JSONObject()
                                        .put("latitude", currentLocation.latitude)
                                        .put("longitude", currentLocation.longitude)
                                )
                                .put("radius", radiusMeters.coerceIn(500.0, 25000.0))
                        )
                    )

                val request = Request.Builder()
                    .url("https://places.googleapis.com/v1/places:searchText")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Goog-Api-Key", BuildConfig.PLACES_API_KEY)
                    .addHeader(
                        "X-Goog-FieldMask",
                        "places.id,places.displayName,places.formattedAddress,places.location"
                    )
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw PlacesRequestFailedException(response.code)
                    }
                    val payload = JSONObject(response.body?.string().orEmpty())
                    val placesArray = payload.optJSONArray("places") ?: return@use emptyList()
                    buildList {
                        repeat(placesArray.length()) { index ->
                            val item = placesArray.getJSONObject(index)
                            val location = item.optJSONObject("location") ?: return@repeat
                            val placeLocation = Location("places").apply {
                                latitude = location.optDouble("latitude")
                                longitude = location.optDouble("longitude")
                            }
                            val distance = currentLocation.distanceTo(placeLocation).toDouble()
                            add(
                                NearbyPlace(
                                    id = item.optString("id"),
                                    name = item.optJSONObject("displayName")?.optString("text").orEmpty(),
                                    address = item.optString("formattedAddress"),
                                    latitude = placeLocation.latitude,
                                    longitude = placeLocation.longitude,
                                    distanceMeters = distance,
                                    estimatedTravelMinutes = TravelTimeEstimator.estimateMinutes(distance, currentLocation)
                                )
                            )
                        }
                    }.sortedBy { it.distanceMeters }
                }
            }
        }
    }

    class MissingPlacesApiKeyException : IllegalStateException("Missing PLACES_API_KEY")

    class PlacesRequestFailedException(val code: Int) :
        IllegalStateException("Places request failed: $code")
}
