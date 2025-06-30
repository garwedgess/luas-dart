package com.wedgess.luas.presentation.map.compose.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
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
import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.domain.model.UserLocation
import kotlinx.collections.immutable.ImmutableList
import timber.log.Timber

private const val DEFAULT_LOCATION_ZOOM = 12.0
private const val DUBLIN_LATITUDE = 53.3498
private const val DUBLIN_LONGITUDE = -6.2603

@SuppressLint("MissingPermission")
@Composable
fun LuasMapLibreMap(
    currentLocation: UserLocation,
    redLineLocations: ImmutableList<LocationEntity.Luas>,
    greenLineLocations: ImmutableList<LocationEntity.Luas>,
    locationPermissionGranted: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    DisposableEffect(Unit) {
        mapView.onCreate(null)

        onDispose {
            (mapView.parent as? ViewGroup)?.removeView(mapView)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            mapView.setupMap(
                currentLocation = currentLocation,
                optionalExtra = { map ->
                    map.style?.let { style ->
                        map.locationComponentSetup(style, context)
                    }
                }
            )
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .zIndex(0f),
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    setupMapWithDefaultLocation()
                    map.setupStyle { style ->
                        style.addLinesMarkersAndLabels(
                            map,
                            "red",
                            redLineLocations,
                            "#E53935",
                            context
                        )
                        style.addLinesMarkersAndLabels(
                            map,
                            "green",
                            greenLineLocations,
                            "#66BF63",
                            context
                        )
                        map.uiSetup()
                    }
                }
            }
        },
        update = { mv ->
            mv.getMapAsync { map ->
                map.style?.let { style ->
                    updateSources(style, "red", redLineLocations)
                    updateSources(style, "green", greenLineLocations)
                }
            }
        }
    )
}

@SuppressLint("MissingPermission")
@Composable
fun DartMapLibreMap(
    currentLocation: UserLocation,
    dartStationLocations: ImmutableList<LocationEntity.Dart>,
    locationPermissionGranted: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    DisposableEffect(Unit) {
        mapView.onCreate(null)

        onDispose {
            (mapView.parent as? ViewGroup)?.removeView(mapView)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            mapView.setupMap(
                currentLocation = currentLocation,
                optionalExtra = { map ->
                    map.style?.let { style ->
                        map.locationComponentSetup(style, context)
                    }
                }
            )
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .zIndex(0f),
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    setupMapWithDefaultLocation()
                    map.setupStyle { style ->
                        style.addLinesMarkersAndLabels(
                            map,
                            "green",
                            dartStationLocations,
                            "#66BF63",
                            context
                        )
                        map.uiSetup()
                    }
                }
            }
        },
        update = { mv ->
            mv.getMapAsync { map ->
                map.style?.let { style ->
                    updateSources(style, "green", dartStationLocations)
                }
            }
        }
    )
}

private fun updateSources(style: Style, line: String, stops: List<LocationEntity>) {
    val lineSource = style.getSourceAs<GeoJsonSource>("$line-line-source")
    if (lineSource != null) {
        val updatedFeatures = FeatureCollection.fromFeatures(
            listOf(
                Feature.fromGeometry(
                    LineString.fromLngLats(
                        stops.map {
                            Point.fromLngLat(
                                it.longitude,
                                it.latitude
                            )
                        }
                    )
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

private fun MapView.setupMap(currentLocation: UserLocation, optionalExtra: ((MapboxMap) -> Unit)? = null) {
    getMapAsync { map ->
        val cameraPosition = CameraPosition.Builder()
            .target(
                LatLng(
                    currentLocation.latitude,
                    currentLocation.longitude
                )
            )
            .zoom(DEFAULT_LOCATION_ZOOM)
            .build()

        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        optionalExtra?.invoke(map)
    }
}

private fun MapView.setupMapWithDefaultLocation() {
    getMapAsync { map ->
        val cameraPosition = CameraPosition.Builder()
            .target(
                LatLng(
                    DUBLIN_LATITUDE,
                    DUBLIN_LONGITUDE
                )
            )
            .zoom(DEFAULT_LOCATION_ZOOM) // Zoom out a bit to show more of the city
            .build()

        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
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
    stops: List<LocationEntity>,
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

private fun Style.addLineSource(line: String, stops: List<LocationEntity>) {
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

private fun Style.addLine(line: String, stops: List<LocationEntity>, color: String) {
    addLineSource(line, stops)
    addLineLayer(line, color)
}

private fun Style.addMarker(line: String, stops: List<LocationEntity>, color: String) {
    addMarkersSource(line, stops)
    addMarkerLayer(line, color)
}

private fun Style.addMarkersSource(line: String, stops: List<LocationEntity>) {
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

private fun Style.addLabelSource(line: String, stop: LocationEntity) {
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
    stops: List<LocationEntity>,
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
    try {
        val locationComponent = locationComponent
        val locationOptions =
            LocationComponentActivationOptions.builder(context, style)
                .useDefaultLocationEngine(true)
                .build()
        locationComponent.activateLocationComponent(locationOptions)
        locationComponent.isLocationComponentEnabled = true
        locationComponent.cameraMode = CameraMode.TRACKING
        locationComponent.renderMode = RenderMode.COMPASS
    } catch (e: Exception) {
        Timber.e(e, "Failed to setup map location component")
    }
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
    text: String
): Bitmap {
    val density = context.resources.displayMetrics.density

    val triangleBaseWidth = 10 * density
    val triangleHeight = 10 * density
    val rectanglePadding = 8 * density
    val cornerRadius = 6 * density
    val tooltipOffset = 30 * density

    val textSizeSp = 14f
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.White.toArgb()
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            textSizeSp,
            context.resources.displayMetrics
        )
        typeface = Typeface.DEFAULT_BOLD
    }

    val textBounds = Rect()
    textPaint.getTextBounds(text, 0, text.length, textBounds)
    val textWidth = textPaint.measureText(text)
    val textHeight = textBounds.height().toFloat()

    val rectangleWidth = textWidth + rectanglePadding * 2
    val rectangleHeight = textHeight + rectanglePadding * 2

    val bitmapWidth = rectangleWidth.toInt()
    val bitmapHeight = (rectangleHeight + triangleHeight).toInt()

    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight + tooltipOffset.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw background
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.Black.copy(alpha = 0.75f).toArgb()
        style = Paint.Style.FILL
    }

    val rectF = RectF(0f, 0f, rectangleWidth, rectangleHeight)
    canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, backgroundPaint)

    // Draw triangle
    val trianglePath = Path().apply {
        moveTo(bitmapWidth / 2f - triangleBaseWidth / 2f, rectangleHeight + tooltipOffset)
        lineTo(bitmapWidth / 2f + triangleBaseWidth / 2f, rectangleHeight + tooltipOffset)
        lineTo(bitmapWidth / 2f, rectangleHeight + triangleHeight + tooltipOffset)
        close()
    }
    canvas.translate(0f, -tooltipOffset)
    canvas.drawPath(trianglePath, backgroundPaint)

    // Draw text centered
    val xText = (rectangleWidth - textWidth) / 2
    val yText = rectanglePadding + textHeight
    canvas.drawText(text, xText, yText + tooltipOffset, textPaint)

    return bitmap
}
