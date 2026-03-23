package com.pingplace.offline

enum class OfflinePackKind {
    CITY,
    AREA
}

enum class OfflinePackSourceType {
    BUNDLED_JSON,
    REMOTE_JSON,
    OVERPASS_BBOX
}

data class OfflinePackDescriptor(
    val id: String,
    val displayName: String,
    val kind: OfflinePackKind,
    val subtitle: String,
    val region: String,
    val sourceType: OfflinePackSourceType,
    val assetPath: String? = null,
    val sourceUrl: String? = null,
    val south: Double? = null,
    val west: Double? = null,
    val north: Double? = null,
    val east: Double? = null
)
