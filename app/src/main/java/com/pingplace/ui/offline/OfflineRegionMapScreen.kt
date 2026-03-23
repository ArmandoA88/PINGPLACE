package com.pingplace.ui.offline

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.json.JSONArray
import org.json.JSONObject

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
                        "Showing ${uiState.places.size} of ${region.placeCount} downloaded stores.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (uiState.isShowingTruncatedPlaces) {
                        Text(
                            "Large regions are capped in the viewer for performance.",
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OfflineRegionMapWebView(
    modifier: Modifier,
    html: String
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadDataWithBaseURL("https://pingplace.local/", html, "text/html", "utf-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://pingplace.local/", html, "text/html", "utf-8", null)
        }
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
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
          <style>
            html, body, #map {
              margin: 0;
              padding: 0;
              height: 100%;
              width: 100%;
              background: #f3efe5;
            }
            .leaflet-popup-content {
              font-family: sans-serif;
              line-height: 1.35;
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
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
