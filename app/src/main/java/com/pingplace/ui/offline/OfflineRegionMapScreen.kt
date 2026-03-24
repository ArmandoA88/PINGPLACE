package com.pingplace.ui.offline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingplace.ui.common.LeafletHtmlMapView
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineRegionMapScreen(
    innerPadding: PaddingValues,
    viewModel: OfflineRegionMapViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(uiState.region?.displayName ?: "Downloaded map") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val errorMessage = uiState.errorMessage
        val region = uiState.region
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = padding.calculateTopPadding())
                .padding(bottom = innerPadding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
                }

                errorMessage != null -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                region != null -> {
                    Text(
                        "Showing ${uiState.places.size} of ${uiState.availablePlaceCount} downloaded stores.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (uiState.isShowingTruncatedPlaces) {
                        Text(
                            "Large regions are capped in the viewer for performance.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    uiState.fallbackMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        "The map shows live OpenStreetMap tiles with your downloaded store markers on top.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OfflineRegionMapWebView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        html = remember(region, uiState.places) {
                            buildMapHtml(
                                region = region,
                                places = uiState.places
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineRegionMapWebView(
    modifier: Modifier,
    html: String
) {
    LeafletHtmlMapView(
        modifier = modifier,
        html = html
    )
}

private fun buildMapHtml(
    region: com.pingplace.data.local.entity.OfflineRegionEntity,
    places: List<com.pingplace.data.local.entity.OfflinePlaceEntity>
): String {
    val placesJson = JSONArray().apply {
        places.forEach { place ->
            put(
                JSONObject().apply {
                    put("name", place.name)
                    put("address", place.address)
                    put("latitude", place.latitude)
                    put("longitude", place.longitude)
                }
            )
        }
    }
    val safeRegionName = JSONObject.quote(region.displayName)
    val fallbackSvg = buildOfflineFallbackSvg(region, places)
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="leaflet/leaflet.css" />
          <style>
            html, body, #frame, #map, #fallback {
              margin: 0;
              padding: 0;
              height: 100%;
              width: 100%;
              background: #f3efe5;
            }
            #frame {
              position: relative;
              overflow: hidden;
            }
            #fallback, #map {
              position: absolute;
              inset: 0;
            }
            #fallback {
              z-index: 1;
            }
            #fallback svg {
              display: block;
              width: 100%;
              height: 100%;
            }
            #map {
              z-index: 2;
              background: transparent;
            }
            .leaflet-popup-content {
              font-family: sans-serif;
              line-height: 1.35;
            }
          </style>
        </head>
        <body>
          <div id="frame">
            <div id="fallback">$fallbackSvg</div>
            <div id="map"></div>
          </div>
          <script src="leaflet/leaflet.js"></script>
          <script>
            const regionName = $safeRegionName;
            const places = $placesJson;
            const map = L.map('map', { zoomControl: true });
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
              attribution: '&copy; OpenStreetMap contributors',
              maxZoom: 19
            }).addTo(map);

            const bounds = L.latLngBounds(
              [${region.minLatitude}, ${region.minLongitude}],
              [${region.maxLatitude}, ${region.maxLongitude}]
            );
            map.fitBounds(bounds.pad(0.08));

            const regionOutline = L.rectangle(bounds, {
              color: '#147d6c',
              weight: 2,
              fillOpacity: 0.04
            }).addTo(map);
            regionOutline.bindTooltip(regionName, { permanent: false });

            places.forEach((place) => {
              const marker = L.circleMarker([place.latitude, place.longitude], {
                radius: 5,
                color: '#147d6c',
                fillColor: '#147d6c',
                fillOpacity: 0.85,
                weight: 1
              }).addTo(map);
              const address = place.address ? `<div>${'$'}{place.address}</div>` : '';
              marker.bindPopup(`<strong>${'$'}{place.name}</strong>${'$'}{address}`);
            });
          </script>
        </body>
        </html>
    """.trimIndent()
}

private fun buildOfflineFallbackSvg(
    region: com.pingplace.data.local.entity.OfflineRegionEntity,
    places: List<com.pingplace.data.local.entity.OfflinePlaceEntity>
): String {
    val width = 1000.0
    val height = 620.0
    val insetLeft = 78.0
    val insetRight = 922.0
    val insetTop = 54.0
    val insetBottom = 566.0

    fun scaleX(longitude: Double): Double {
        val normalized = (longitude - region.minLongitude) /
            (region.maxLongitude - region.minLongitude).coerceAtLeast(0.000001)
        return insetLeft + normalized * (insetRight - insetLeft)
    }

    fun scaleY(latitude: Double): Double {
        val normalized = (latitude - region.minLatitude) /
            (region.maxLatitude - region.minLatitude).coerceAtLeast(0.000001)
        return insetBottom - normalized * (insetBottom - insetTop)
    }

    fun escaped(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    fun coordinate(value: Double): String = String.format(Locale.US, "%.1f", value)

    val markers = buildString {
        places.take(250).forEach { place ->
            append(
                """
                <circle cx="${coordinate(scaleX(place.longitude))}" cy="${coordinate(scaleY(place.latitude))}" r="5.5" fill="#147D6C" fill-opacity="0.86"/>
                """.trimIndent()
            )
        }
    }

    return """
        <svg viewBox="0 0 ${coordinate(width)} ${coordinate(height)}" xmlns="http://www.w3.org/2000/svg" aria-label="Offline fallback map">
          <rect x="0" y="0" width="${coordinate(width)}" height="${coordinate(height)}" fill="#F3EFE5"/>
          <rect x="${coordinate(insetLeft)}" y="${coordinate(insetTop)}" width="${coordinate(insetRight - insetLeft)}" height="${coordinate(insetBottom - insetTop)}" rx="28" fill="#F8F3EA" stroke="#DDD3BF" stroke-width="3"/>
          <text x="${coordinate(insetLeft)}" y="34.0" fill="#6A746E" font-size="24" font-weight="700">${escaped(region.displayName)}</text>
          <text x="${coordinate(insetLeft)}" y="602.0" fill="#8B948F" font-size="20">Downloaded stores plotted locally while map tiles load.</text>
          $markers
        </svg>
    """.trimIndent()
}
