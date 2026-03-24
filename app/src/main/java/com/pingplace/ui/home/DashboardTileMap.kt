package com.pingplace.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.max
import kotlin.math.roundToInt

private val OpenStreetMapTileSource = XYTileSource(
    "OpenStreetMap",
    0,
    19,
    256,
    ".png",
    arrayOf("https://tile.openstreetmap.org/")
)

@Composable
fun DashboardTileMap(
    modifier: Modifier = Modifier,
    userLatitude: Double,
    userLongitude: Double,
    places: List<NearbyStoreUiModel>
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember(context) {
        MapView(context).apply {
            setTileSource(OpenStreetMapTileSource)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            setUseDataConnection(true)
            setScrollableAreaLimitLatitude(MapView.getTileSystem().maxLatitude, MapView.getTileSystem().minLatitude, 0)
            setScrollableAreaLimitLongitude(MapView.getTileSystem().minLongitude, MapView.getTileSystem().maxLongitude, 0)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
        }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(modifier = modifier.clip(RoundedCornerShape(22.dp))) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { view ->
                renderDashboardMap(
                    mapView = view,
                    context = context,
                    userLatitude = userLatitude,
                    userLongitude = userLongitude,
                    places = places
                )
            }
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            shadowElevation = 0.dp
        ) {
            Text(
                text = "\u00A9 OpenStreetMap contributors",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun renderDashboardMap(
    mapView: MapView,
    context: Context,
    userLatitude: Double,
    userLongitude: Double,
    places: List<NearbyStoreUiModel>
) {
    val userPoint = GeoPoint(userLatitude, userLongitude)
    val points = buildList {
        add(userPoint)
        places.forEach { add(GeoPoint(it.latitude, it.longitude)) }
    }
    val palette = listOf(
        0xFF1E6B5C.toInt(),
        0xFFBF5A3D.toInt(),
        0xFF3D6BBF.toInt(),
        0xFF8C6A1C.toInt(),
        0xFF7A4FA3.toInt()
    )
    val brandColors = linkedMapOf<String, Int>()

    mapView.overlays.clear()
    mapView.minZoomLevel = 3.0
    mapView.maxZoomLevel = 19.0

    val userMarker = Marker(mapView).apply {
        position = userPoint
        icon = buildUserMarkerDrawable(context)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        title = "You"
        snippet = "Current location"
    }
    mapView.overlays.add(userMarker)

    places.forEach { place ->
        val color = brandColors.getOrPut(place.brandQuery) {
            palette[brandColors.size % palette.size]
        }
        mapView.overlays.add(
            Marker(mapView).apply {
                position = GeoPoint(place.latitude, place.longitude)
                icon = buildStoreMarkerDrawable(context, color)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = place.name
                snippet = buildString {
                    append(place.brandName)
                    if (place.address.isNotBlank()) {
                        append('\n')
                        append(place.address)
                    }
                }
            }
        )
    }

    if (points.size == 1) {
        mapView.controller.setZoom(14.0)
        mapView.controller.setCenter(userPoint)
    } else {
        mapView.post {
            mapView.zoomToBoundingBox(buildBoundingBox(points), true)
        }
    }

    mapView.invalidate()
}

private fun buildBoundingBox(points: List<GeoPoint>): BoundingBox {
    val latitudes = points.map { it.latitude }
    val longitudes = points.map { it.longitude }
    val minLatitude = latitudes.min()
    val maxLatitude = latitudes.max()
    val minLongitude = longitudes.min()
    val maxLongitude = longitudes.max()
    val latitudeSpan = max(maxLatitude - minLatitude, 0.006)
    val longitudeSpan = max(maxLongitude - minLongitude, 0.006)
    val latitudePadding = latitudeSpan * 0.20
    val longitudePadding = longitudeSpan * 0.20
    return BoundingBox(
        maxLatitude + latitudePadding,
        maxLongitude + longitudePadding,
        minLatitude - latitudePadding,
        minLongitude - longitudePadding
    )
}

private fun buildStoreMarkerDrawable(
    context: Context,
    fillColor: Int
): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (18 * density).roundToInt().coerceAtLeast(1)
    val center = sizePx / 2f
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = 0xFFFFFFFF.toInt()
    paint.style = Paint.Style.FILL
    canvas.drawCircle(center, center, sizePx * 0.46f, paint)

    paint.color = fillColor
    canvas.drawCircle(center, center, sizePx * 0.30f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun buildUserMarkerDrawable(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (34 * density).roundToInt().coerceAtLeast(1)
    val center = sizePx / 2f
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.style = Paint.Style.FILL
    paint.color = 0x88BCD1EE.toInt()
    canvas.drawCircle(center, center, sizePx * 0.46f, paint)

    paint.color = 0xCCDCE8F7.toInt()
    canvas.drawCircle(center, center, sizePx * 0.31f, paint)

    paint.color = 0xFF2F6DC0.toInt()
    canvas.drawCircle(center, center, sizePx * 0.16f, paint)

    paint.color = 0xFFFFFFFF.toInt()
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = sizePx * 0.07f
    canvas.drawCircle(center, center, sizePx * 0.16f, paint)

    paint.style = Paint.Style.FILL
    paint.color = 0xFF21456C.toInt()
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = sizePx * 0.22f
    canvas.drawText("You", center, sizePx * 0.18f, paint)

    return BitmapDrawable(context.resources, bitmap)
}
