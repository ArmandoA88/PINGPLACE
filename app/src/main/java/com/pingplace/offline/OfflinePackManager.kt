package com.pingplace.offline

import android.content.Context
import com.pingplace.BuildConfig
import androidx.room.withTransaction
import com.pingplace.data.local.PingPlaceDatabase
import com.pingplace.data.local.entity.OfflinePlaceEntity
import com.pingplace.data.local.entity.OfflineRegionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OfflinePackManager(
    context: Context,
    private val database: PingPlaceDatabase,
    private val client: OkHttpClient = OkHttpClient()
) {

    private val appContext = context.applicationContext

    fun observeInstalledRegions(): Flow<List<OfflineRegionEntity>> =
        database.offlineRegionDao().observeAll()

    suspend fun getRegion(regionId: String): OfflineRegionEntity? = withContext(Dispatchers.IO) {
        database.offlineRegionDao().getById(regionId)
    }

    suspend fun getRegionPlaces(regionId: String, limit: Int = MAP_VIEW_PLACE_LIMIT): List<OfflinePlaceEntity> =
        withContext(Dispatchers.IO) {
            database.offlinePlaceDao().getByRegion(regionId, limit)
        }

    suspend fun getRegionPlaceCount(regionId: String): Int = withContext(Dispatchers.IO) {
        database.offlinePlaceDao().countByRegion(regionId)
    }

    suspend fun getPlacesInBounds(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double,
        limit: Int = MAP_VIEW_PLACE_LIMIT
    ): List<OfflinePlaceEntity> = withContext(Dispatchers.IO) {
        database.offlinePlaceDao().getInBounds(
            minLatitude = minLatitude,
            maxLatitude = maxLatitude,
            minLongitude = minLongitude,
            maxLongitude = maxLongitude,
            limit = limit
        )
    }

    suspend fun countPlacesInBounds(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): Int = withContext(Dispatchers.IO) {
        database.offlinePlaceDao().countInBounds(
            minLatitude = minLatitude,
            maxLatitude = maxLatitude,
            minLongitude = minLongitude,
            maxLongitude = maxLongitude
        )
    }

    suspend fun refreshInstalledRegion(regionId: String): Result<OfflineRegionEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val descriptor = loadCatalog()
                .getOrThrow()
                .firstOrNull { it.id == regionId }
                ?: error("Offline pack definition for $regionId is unavailable.")
            importCatalogPack(descriptor).getOrThrow()
        }
    }

    suspend fun loadCatalog(): Result<List<OfflinePackDescriptor>> = withContext(Dispatchers.IO) {
        runCatching {
            val remoteUrl = BuildConfig.OFFLINE_PACK_MANIFEST_URL.trim()
            if (remoteUrl.isNotBlank()) {
                val remote = fetchCatalogFromUrl(remoteUrl)
                if (remote.isNotEmpty()) {
                    return@runCatching remote
                }
            }
            loadCatalogFromAssets()
        }
    }

    suspend fun importCatalogPack(descriptor: OfflinePackDescriptor): Result<OfflineRegionEntity> = withContext(Dispatchers.IO) {
        runCatching {
            when (descriptor.sourceType) {
                OfflinePackSourceType.REMOTE_JSON -> {
                    require(!descriptor.sourceUrl.isNullOrBlank()) { "Pack URL is missing." }
                    val request = Request.Builder().url(descriptor.sourceUrl).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) error("Pack download failed: ${response.code}")
                        val payload = response.body?.string().orEmpty()
                        importPackJson(payload, sourceUrl = descriptor.sourceUrl)
                    }
                }
                OfflinePackSourceType.BUNDLED_JSON -> {
                    require(!descriptor.assetPath.isNullOrBlank()) { "Pack asset is missing." }
                    val payload = appContext.assets.open(descriptor.assetPath).bufferedReader().use { it.readText() }
                    importPackJson(payload, sourceUrl = "asset://${descriptor.assetPath}")
                }
                OfflinePackSourceType.OVERPASS_BBOX -> importOverpassPack(descriptor)
            }
        }
    }

    suspend fun importPackFromUrl(url: String): Result<OfflineRegionEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Pack download failed: ${response.code}")
                val payload = response.body?.string().orEmpty()
                importPackJson(payload, sourceUrl = url)
            }
        }
    }

    suspend fun removeLegacySamplePacks(): Int = withContext(Dispatchers.IO) {
        val legacyIds = database.offlineRegionDao().getLegacySampleIds(LEGACY_SAMPLE_UPDATED_AT_EPOCH_MILLIS)
        if (legacyIds.isEmpty()) return@withContext 0
        database.withTransaction {
            legacyIds.forEach { regionId ->
                database.offlinePlaceDao().deleteByRegion(regionId)
                database.offlineRegionDao().deleteById(regionId)
            }
        }
        legacyIds.size
    }

    suspend fun removePack(regionId: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            database.offlinePlaceDao().deleteByRegion(regionId)
            database.offlineRegionDao().deleteById(regionId)
        }
    }

    private suspend fun importPackJson(payload: String, sourceUrl: String): OfflineRegionEntity {
        val root = JSONObject(payload)
        val regionJson = root.getJSONObject("region")
        val placesJson = root.getJSONArray("places")
        val places = parsePlaces(
            regionId = regionJson.getString("id"),
            placesJson = placesJson
        )
        val bounds = computeBounds(regionJson, places)
        val region = OfflineRegionEntity(
            id = regionJson.getString("id"),
            displayName = regionJson.optString("displayName").ifBlank { regionJson.getString("id") },
            downloadedAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = regionJson.optLong("updatedAtEpochMillis").takeIf { it > 0L },
            sourceUrl = sourceUrl,
            placeCount = places.size,
            minLatitude = bounds.minLatitude,
            maxLatitude = bounds.maxLatitude,
            minLongitude = bounds.minLongitude,
            maxLongitude = bounds.maxLongitude
        )
        database.withTransaction {
            database.offlinePlaceDao().deleteByRegion(region.id)
            database.offlineRegionDao().insert(region)
            places.chunked(500).forEach { chunk ->
                database.offlinePlaceDao().insertAll(chunk)
            }
        }
        return region
    }

    private suspend fun importOverpassPack(descriptor: OfflinePackDescriptor): OfflineRegionEntity {
        val south = requireNotNull(descriptor.south)
        val west = requireNotNull(descriptor.west)
        val north = requireNotNull(descriptor.north)
        val east = requireNotNull(descriptor.east)
        val query = buildOverpassQuery(south, west, north, east)
        val request = Request.Builder()
            .url(OVERPASS_INTERPRETER_URL)
            .post(query.toRequestBody("text/plain".toMediaType()))
            .build()

        val places = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Overpass download failed: ${response.code}")
            parseOverpassPlaces(
                regionId = descriptor.id,
                payload = response.body?.string().orEmpty()
            )
        }
        val region = OfflineRegionEntity(
            id = descriptor.id,
            displayName = descriptor.displayName,
            downloadedAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
            sourceUrl = OVERPASS_INTERPRETER_URL,
            placeCount = places.size,
            minLatitude = south,
            maxLatitude = north,
            minLongitude = west,
            maxLongitude = east
        )
        database.withTransaction {
            database.offlinePlaceDao().deleteByRegion(region.id)
            database.offlineRegionDao().insert(region)
            places.chunked(500).forEach { chunk ->
                database.offlinePlaceDao().insertAll(chunk)
            }
        }
        return region
    }

    private fun parsePlaces(regionId: String, placesJson: JSONArray): List<OfflinePlaceEntity> {
        return buildList {
            repeat(placesJson.length()) { index ->
                val item = placesJson.getJSONObject(index)
                add(
                    OfflinePlaceEntity(
                        id = scopedPlaceId(regionId, item.getString("id")),
                        regionId = regionId,
                        name = item.getString("name"),
                        address = item.optString("address"),
                        latitude = item.getDouble("latitude"),
                        longitude = item.getDouble("longitude"),
                        searchText = normalizeSearchText(
                            item.optString("searchText").ifBlank {
                                listOf(
                                    item.optString("name"),
                                    item.optString("brand"),
                                    item.optString("category"),
                                    item.optString("address")
                                ).joinToString(" ")
                            }
                        )
                    )
                )
            }
        }
    }

    private fun parseOverpassPlaces(regionId: String, payload: String): List<OfflinePlaceEntity> {
        val root = JSONObject(payload)
        val elements = root.optJSONArray("elements") ?: JSONArray()
        val seen = linkedSetOf<String>()
        return buildList {
            repeat(elements.length()) { index ->
                val item = elements.getJSONObject(index)
                val tags = item.optJSONObject("tags") ?: return@repeat
                val lat = when {
                    item.has("lat") -> item.optDouble("lat")
                    item.has("center") -> item.getJSONObject("center").optDouble("lat")
                    else -> Double.NaN
                }
                val lon = when {
                    item.has("lon") -> item.optDouble("lon")
                    item.has("center") -> item.getJSONObject("center").optDouble("lon")
                    else -> Double.NaN
                }
                if (lat.isNaN() || lon.isNaN()) return@repeat
                val name = tags.optString("name").ifBlank {
                    tags.optString("brand").ifBlank {
                        tags.optString("shop").ifBlank { tags.optString("amenity") }
                    }
                }
                if (name.isBlank()) return@repeat
                val id = scopedPlaceId(regionId, "osm:${item.optString("type")}:${item.optLong("id")}")
                if (!seen.add(id)) return@repeat
                val address = listOf(
                    tags.optString("addr:housenumber"),
                    tags.optString("addr:street")
                ).filter { it.isNotBlank() }.joinToString(" ").ifBlank {
                    tags.optString("addr:full")
                }
                val searchText = listOf(
                    name,
                    tags.optString("brand"),
                    tags.optString("shop"),
                    tags.optString("amenity"),
                    address
                ).joinToString(" ")
                add(
                    OfflinePlaceEntity(
                        id = id,
                        regionId = regionId,
                        name = name,
                        address = address,
                        latitude = lat,
                        longitude = lon,
                        searchText = normalizeSearchText(searchText)
                    )
                )
            }
        }
    }

    private fun buildOverpassQuery(south: Double, west: Double, north: Double, east: Double): String {
        val bbox = "$south,$west,$north,$east"
        return """
            [out:json][timeout:120];
            (
              nwr["shop"]($bbox);
              nwr["amenity"~"^(pharmacy|post_office|fuel|marketplace)$"]($bbox);
            );
            out center tags;
        """.trimIndent()
    }

    private fun computeBounds(
        regionJson: JSONObject,
        places: List<OfflinePlaceEntity>
    ): Bounds {
        if (
            regionJson.has("minLatitude") &&
            regionJson.has("maxLatitude") &&
            regionJson.has("minLongitude") &&
            regionJson.has("maxLongitude")
        ) {
            return Bounds(
                minLatitude = regionJson.getDouble("minLatitude"),
                maxLatitude = regionJson.getDouble("maxLatitude"),
                minLongitude = regionJson.getDouble("minLongitude"),
                maxLongitude = regionJson.getDouble("maxLongitude")
            )
        }
        require(places.isNotEmpty()) { "Offline pack must include at least one place." }
        return Bounds(
            minLatitude = places.minOf { it.latitude },
            maxLatitude = places.maxOf { it.latitude },
            minLongitude = places.minOf { it.longitude },
            maxLongitude = places.maxOf { it.longitude }
        )
    }

    private fun normalizeSearchText(text: String): String =
        text.lowercase().replace(Regex("\\s+"), " ").trim()

    private fun scopedPlaceId(regionId: String, rawId: String): String = "$regionId::$rawId"

    private fun fetchCatalogFromUrl(url: String): List<OfflinePackDescriptor> {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Catalog download failed: ${response.code}")
            return parseCatalog(response.body?.string().orEmpty())
        }
    }

    private fun loadCatalogFromAssets(): List<OfflinePackDescriptor> {
        val payload = appContext.assets.open("offline-packs/catalog.json")
            .bufferedReader()
            .use { it.readText() }
        return parseCatalog(payload)
    }

    private fun parseCatalog(payload: String): List<OfflinePackDescriptor> {
        val root = JSONObject(payload)
        val packs = root.getJSONArray("packs")
        return buildList {
            repeat(packs.length()) { index ->
                val item = packs.getJSONObject(index)
                add(
                    OfflinePackDescriptor(
                        id = item.getString("id"),
                        displayName = item.getString("displayName"),
                        kind = OfflinePackKind.valueOf(item.getString("kind")),
                        subtitle = item.optString("subtitle"),
                        region = item.optString("region"),
                        sourceType = item.optString("sourceType")
                            .takeIf { it.isNotBlank() }
                            ?.let(OfflinePackSourceType::valueOf)
                            ?: if (item.optString("sourceUrl").isNotBlank()) {
                                OfflinePackSourceType.REMOTE_JSON
                            } else {
                                OfflinePackSourceType.BUNDLED_JSON
                            },
                        assetPath = item.optString("assetPath").takeIf { it.isNotBlank() },
                        sourceUrl = item.optString("sourceUrl").takeIf { it.isNotBlank() },
                        south = item.takeIf { it.has("south") }?.getDouble("south"),
                        west = item.takeIf { it.has("west") }?.getDouble("west"),
                        north = item.takeIf { it.has("north") }?.getDouble("north"),
                        east = item.takeIf { it.has("east") }?.getDouble("east")
                    )
                )
            }
        }
    }

    private data class Bounds(
        val minLatitude: Double,
        val maxLatitude: Double,
        val minLongitude: Double,
        val maxLongitude: Double
    )

    private companion object {
        const val OVERPASS_INTERPRETER_URL = "https://overpass-api.de/api/interpreter"
        const val LEGACY_SAMPLE_UPDATED_AT_EPOCH_MILLIS = 1_770_000_000_000L
        const val MAP_VIEW_PLACE_LIMIT = 1_500
    }
}
