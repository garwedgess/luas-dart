package com.wedgess.luas.presentation.map.compose.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeWidth
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth
import com.mapbox.mapboxsdk.style.layers.RasterLayer
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.style.sources.RasterSource
import com.mapbox.mapboxsdk.style.sources.TileSet
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.presentation.map.MapContract

@SuppressLint("MissingPermission")
@Composable
fun MapLibreMap(uiState: MapContract.UiState) {

    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    DisposableEffect(Unit) {
        mapView.onCreate(null)
        mapView.apply {
            getMapAsync { map ->
                setupMap(uiState)
                map.setupStyle { style ->
                    style.addLinesMarkersAndLabels(map, "red", uiState.redLineLocations, "#E53935", context)
                    style.addLinesMarkersAndLabels(map, "green", uiState.greenLineLocations, "#66BF63", context)
                    map.locationComponentSetup(style, context)
                    map.uiSetup()
                }
            }
        }

        onDispose {
            (mapView.parent as? ViewGroup)?.removeView(mapView)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(0f),
        factory = { mapView },
        update = { mv ->
            mv.getMapAsync { map ->
                map.style?.let { style ->
                    updateSources(style, "red", uiState.redLineLocations)
                    updateSources(style, "green", uiState.greenLineLocations)
                }
            }
        }
    )
}

private fun updateSources(style: Style, line: String, stops: List<StopEntity>) {
    val lineSource = style.getSourceAs<GeoJsonSource>("$line-line-source")
    if (lineSource != null) {
        val updatedFeatures = FeatureCollection.fromFeatures(
            listOf(
                Feature.fromGeometry(
                    LineString.fromLngLats(stops.map {
                        Point.fromLngLat(
                            it.longitude,
                            it.latitude
                        )
                    })
                )
            )
        )
        lineSource.setGeoJson(updatedFeatures)
    }
    val markerSource = style.getSourceAs<GeoJsonSource>("$line-markers-source")
    if (markerSource != null) {
        val updatedMarkerFeatures =
            FeatureCollection.fromFeatures(
                stops.map {
                    Feature.fromGeometry(
                        Point.fromLngLat(
                            it.longitude,
                            it.latitude
                        )
                    ).apply {
                        addStringProperty("name", it.name)
                    }
                }
            )
        markerSource.setGeoJson(updatedMarkerFeatures)
    }
}

private fun MapView.setupMap(uiState: MapContract.UiState) {
    getMapAsync { map ->
        val cameraPosition = CameraPosition.Builder()
            .target(
                com.mapbox.mapboxsdk.geometry.LatLng(
                    uiState.currentLocation.latitude,
                    uiState.currentLocation.longitude
                )
            )
            .zoom(18.0)
            .build()

        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }
}

private fun MapboxMap.setupStyle(onSetup: (Style) -> Unit) {
    val osmTileUrl = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
    val rasterSource = RasterSource("osm-source", TileSet("tileset", osmTileUrl))
    val rasterLayer = RasterLayer("osm-layer", "osm-source")

    this.setStyle(
        Style.Builder()
            .withSource(rasterSource)
            .withLayer(rasterLayer)
    ) { style ->
        onSetup(style)
    }
}

private fun Style.addLinesMarkersAndLabels(
    map: MapboxMap,
    line: String,
    stops: List<StopEntity>,
    color: String,
    context: Context
) {
    addLine(line, stops, color)
    addMarker(line, stops, color)
    addLabels(map, line, stops, context)
}

private fun Style.addLineLayer(line: String, color: String) {
    val redLineLayer = LineLayer("$line-line-layer", "$line-line-source")
        .withProperties(
            lineColor(color),
            lineWidth(5f)
        )
    addLayer(redLineLayer)
}

private fun Style.addLineSource(line: String, stops: List<StopEntity>) {
    val lineSource = GeoJsonSource(
        "$line-line-source",
        FeatureCollection.fromFeatures(
            listOf(
                Feature.fromGeometry(
                    LineString.fromLngLats(
                        stops.map { Point.fromLngLat(it.longitude, it.latitude) }
                    )
                )
            )
        )
    )
    addSource(lineSource)
}

private fun Style.addLine(line: String, stops: List<StopEntity>, color: String) {
    addLineSource(line, stops)
    addLineLayer(line, color)
}

private fun Style.addMarker(line: String, stops: List<StopEntity>, color: String) {
    addMarkersSource(line, stops)
    addMarkerLayer(line, color)
}

private fun Style.addMarkersSource(line: String, stops: List<StopEntity>) {
    val markersSource = GeoJsonSource(
        "$line-markers-source",
        FeatureCollection.fromFeatures(
            stops.map {
                Feature.fromGeometry(
                    Point.fromLngLat(
                        it.longitude,
                        it.latitude
                    )
                ).apply {
                    addStringProperty("name", it.name)
                }
            }
        )
    )
    addSource(markersSource)
}

private fun Style.addMarkerLayer(line: String, color: String) {
    val circleLayer = CircleLayer("$line-circle-layer", "$line-markers-source")
        .withProperties(
            circleRadius(8f),
            circleColor(color),
            circleStrokeWidth(2f),
            circleStrokeColor("#ffffff"),
            circleOpacity(1f)
        )
    addLayer(circleLayer)
}

private fun Style.addLabelSource(line: String, stop: StopEntity) {
    val labelSource = GeoJsonSource(
        "$line-markers-source-${stop.name}",
        FeatureCollection.fromFeatures(
            listOf(
                Feature.fromGeometry(Point.fromLngLat(stop.longitude, stop.latitude)).apply {
                    addStringProperty("name", stop.name)
                }
            )
        )
    )
    addSource(labelSource)
}

private fun Style.addLabelLayer(line: String, stopName: String) {
    // Use a SymbolLayer to render the markers
    val labelLayer = SymbolLayer(
        "$line-icon-layer-$stopName",
        "$line-markers-source-$stopName"
    ).apply {
        withProperties(
            iconImage("$line-marker-icon-$stopName"),
            iconSize(
                Expression.interpolate(
                    Expression.exponential(1f),
                    Expression.zoom(),
                    Expression.stop(10, 0.5f),
                    Expression.stop(16, 1.5f)
                )
            ),
            iconAllowOverlap(true),
            PropertyFactory.iconOffset(arrayOf(0f, -3.5f))
        )
    }
    addLayerAbove(labelLayer, "$line-circle-layer")
}

private fun Style.addLabels(
    map: MapboxMap,
    line: String,
    stops: List<StopEntity>,
    context: Context
) {
    stops.forEach { location ->
        val customMarkerBitmap = createTooltipBitmap(context, location.name)
        addImage("$line-marker-icon-${location.name}", customMarkerBitmap)

        addLabelSource(line, location)
        addLabelLayer(line, location.name)
        map.addCameraListener(this, line, location.name)
    }
}

private fun MapboxMap.addCameraListener(style: Style, line: String, stopName: String) {
    val zoomThreshold = 10f
    addOnCameraIdleListener {
        val zoomLevel = cameraPosition.zoom

        val labelVisibility =
            if (zoomLevel >= zoomThreshold) "visible" else "none"
        val markerVisibility =
            if (zoomLevel >= (zoomThreshold - 2)) "visible" else "none"

        style.getLayer("$line-icon-layer-$stopName")?.setProperties(
            PropertyFactory.visibility(labelVisibility)
        )

        style.getLayer("$line-circle-layer")?.setProperties(
            PropertyFactory.visibility(markerVisibility)
        )
    }
}

@SuppressLint("MissingPermission")
private fun MapboxMap.locationComponentSetup(style: Style, context: Context) {
    val locationComponent = locationComponent
    val locationOptions =
        LocationComponentActivationOptions.builder(context, style)
            .useDefaultLocationEngine(true)
            .build()
    locationComponent.activateLocationComponent(locationOptions)
    locationComponent.isLocationComponentEnabled = true
    locationComponent.cameraMode = CameraMode.TRACKING
    locationComponent.renderMode = RenderMode.COMPASS
}

private fun MapboxMap.uiSetup() {
    uiSettings.apply {
        isCompassEnabled = true
        isRotateGesturesEnabled = true
        isTiltGesturesEnabled = true
        isScrollGesturesEnabled = true
    }
}

private fun createTooltipBitmap(
    context: Context,
    text: String,
): Bitmap {
    val triangleBaseWidthDp = 10.dp // Width of the triangle's base
    val triangleHeightDp = 10.dp // Height of the triangle
    val rectanglePaddingDp = 4.dp
    val textStyle = TextStyle(
        color = androidx.compose.ui.graphics.Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )

    val triangleBaseWidth =
        with(androidx.compose.ui.unit.Density(context.resources.displayMetrics.density)) { triangleBaseWidthDp.toPx() }
    val triangleHeight =
        with(androidx.compose.ui.unit.Density(context.resources.displayMetrics.density)) { triangleHeightDp.toPx() }
    val rectanglePadding =
        with(androidx.compose.ui.unit.Density(context.resources.displayMetrics.density)) { rectanglePaddingDp.toPx() }
    val tooltipOffset =
        with(androidx.compose.ui.unit.Density(context.resources.displayMetrics.density)) { 30.dp.toPx() }

    val rectanglePaint = Paint().apply {
        color = android.graphics.Color.argb(150, 0, 0, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = textStyle.fontSize.value * context.resources.displayMetrics.scaledDensity
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val textBounds = Rect()
    textPaint.getTextBounds(text, 0, text.length, textBounds)

    val rectangleWidth = textBounds.width() + 2 * rectanglePadding
    val rectangleHeight = textBounds.height() + 2 * rectanglePadding
    val bitmapWidth = rectangleWidth.toInt()
    val bitmapHeight = (rectangleHeight + triangleHeight).toInt()

    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight + tooltipOffset.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val rectangleX = 0f

    val tooltipPath = Path().apply {
        // Rectangle
        moveTo(rectangleX, tooltipOffset)
        lineTo(rectangleX + rectangleWidth, tooltipOffset)
        lineTo(rectangleX + rectangleWidth, tooltipOffset + rectangleHeight)

        // Triangle
        lineTo(bitmapWidth / 2f + triangleBaseWidth / 2f, tooltipOffset + rectangleHeight)
        lineTo(bitmapWidth / 2f, tooltipOffset + rectangleHeight + triangleHeight)
        lineTo(bitmapWidth / 2f - triangleBaseWidth / 2f, tooltipOffset + rectangleHeight)
        lineTo(rectangleX, tooltipOffset + rectangleHeight)

        close()
    }
    canvas.translate(0f, -tooltipOffset)
    canvas.drawPath(tooltipPath, rectanglePaint)

    canvas.drawText(
        text,
        rectangleX + rectanglePadding,
        tooltipOffset + rectanglePadding + textBounds.height(),
        textPaint
    )

    return bitmap
}
