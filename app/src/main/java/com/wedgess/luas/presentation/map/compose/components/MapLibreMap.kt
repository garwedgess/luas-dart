package com.wedgess.luas.presentation.map.compose.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.domain.model.UserLocation
import kotlinx.collections.immutable.ImmutableList
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import timber.log.Timber

private const val ROUTE_WIDTH = 4f
private const val MARKER_RADIUS = 6f
private const val SELECTED_MARKER_RADIUS = 11f
private const val LABEL_SIZE = 13f
private const val LABEL_OFFSET = 1.2f
private const val DEFAULT_LOCATION_ZOOM = 12.0
private const val STATION_ZOOM = 14.0
private const val STATION_HIT_RADIUS_DP = 24f
private const val DUBLIN_LATITUDE = 53.3498
private const val DUBLIN_LONGITUDE = -6.2603

// Provider configuration. MapLibre retains the style's attribution control.
internal const val LIGHT_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
internal const val DARK_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/dark"
private const val DARK_SURFACE_LUMINANCE = 0.5f

@Suppress("LongParameterList") // Stateless map input and callbacks.
@Composable
fun LuasMapLibreMap(
    currentLocation: UserLocation,
    redLineLocations: ImmutableList<LocationEntity.Luas>,
    greenLineLocations: ImmutableList<LocationEntity.Luas>,
    locationPermissionGranted: Boolean,
    modifier: Modifier = Modifier,
    selectedStation: LocationEntity? = null,
    onStationSelect: (LocationEntity) -> Unit = {}
) {
    TransitMap(
        groups = listOf(MapLine("red", "#C62828", redLineLocations), MapLine("green", "#287D3C", greenLineLocations)),
        currentLocation = currentLocation,
        locationPermissionGranted = locationPermissionGranted,
        selectedStation = selectedStation,
        onStationSelect = onStationSelect,
        modifier = modifier
    )
}

@Suppress("LongParameterList") // Stateless map input and callbacks.
@Composable
fun DartMapLibreMap(
    currentLocation: UserLocation,
    dartStationLocations: ImmutableList<LocationEntity.Dart>,
    locationPermissionGranted: Boolean,
    modifier: Modifier = Modifier,
    selectedStation: LocationEntity? = null,
    onStationSelect: (LocationEntity) -> Unit = {}
) {
    TransitMap(
        groups = listOf(MapLine("dart", "#287D3C", dartStationLocations)),
        currentLocation = currentLocation,
        locationPermissionGranted = locationPermissionGranted,
        selectedStation = selectedStation,
        onStationSelect = onStationSelect,
        modifier = modifier
    )
}

private data class MapLine(val id: String, val color: String, val stations: List<LocationEntity>)

@Suppress("LongParameterList") // Shared renderer for both transport types.
@SuppressLint("MissingPermission")
@Composable
private fun TransitMap(
    groups: List<MapLine>,
    currentLocation: UserLocation,
    locationPermissionGranted: Boolean,
    selectedStation: LocationEntity?,
    onStationSelect: (LocationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE
    val styleUrl = if (dark) DARK_MAP_STYLE_URL else LIGHT_MAP_STYLE_URL
    val mapView = rememberMapView()
    val latestGroups = rememberUpdatedState(groups)
    val latestSelection = rememberUpdatedState(selectedStation)
    val latestOnSelect = rememberUpdatedState(onStationSelect)
    val latestPermission = rememberUpdatedState(locationPermissionGranted)

    DisposableEffect(mapView) {
        val listener = MapLibreMap.OnMapClickListener { point ->
            mapView.getMapAsync { map ->
                val pixel = map.projection.toScreenLocation(point)
                val radius = STATION_HIT_RADIUS_DP * context.resources.displayMetrics.density
                val bounds = RectF(pixel.x - radius, pixel.y - radius, pixel.x + radius, pixel.y + radius)
                val layers = latestGroups.value.flatMap { listOf("${it.id}-stations", "${it.id}-labels") }

                @Suppress("SpreadOperator") // Native SDK accepts layer ids only as varargs.
                val code = map.queryRenderedFeatures(bounds, *layers.toTypedArray())
                    .firstOrNull()?.getStringProperty("stationCode")
                latestGroups.value.flatMap { it.stations }.firstOrNull { it.stationCode == code }?.let {
                    latestOnSelect.value(it)
                }
            }
            true
        }
        mapView.getMapAsync { it.addOnMapClickListener(listener) }
        onDispose { mapView.getMapAsync { it.removeOnMapClickListener(listener) } }
    }

    LaunchedEffect(locationPermissionGranted) {
        mapView.getMapAsync { map -> map.style?.let { map.updateLocation(it, context, locationPermissionGranted) } }
    }
    LaunchedEffect(selectedStation) {
        selectedStation?.let {
            mapView.getMapAsync { map ->
                if (map.locationComponent.isLocationComponentActivated) {
                    map.locationComponent.cameraMode = CameraMode.NONE
                }
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), STATION_ZOOM))
            }
        }
    }

    LaunchedEffect(styleUrl) {
        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                latestGroups.value.forEach { style.addTransportLayers(it, latestSelection.value, dark) }
                map.updateLocation(style, context, latestPermission.value, followUser = latestSelection.value == null)
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    val location = currentLocation.takeUnless { it.latitude == 0.0 && it.longitude == 0.0 }
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(location?.latitude ?: DUBLIN_LATITUDE, location?.longitude ?: DUBLIN_LONGITUDE),
                            DEFAULT_LOCATION_ZOOM
                        )
                    )
                }
            }
        },
        update = { view ->
            view.getMapAsync { map ->
                map.style?.let { style -> groups.forEach { style.updateTransportSources(it, selectedStation) } }
            }
        }
    )
}

@Composable
private fun rememberMapView(): MapView {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            (mapView.parent as? ViewGroup)?.removeView(mapView)
            mapView.onDestroy()
        }
    }
    return mapView
}

private fun MapLine.markerFeatures(selected: LocationEntity?): FeatureCollection = FeatureCollection.fromFeatures(
    stations.map { station ->
        Feature.fromGeometry(Point.fromLngLat(station.longitude, station.latitude)).apply {
            addStringProperty("name", station.name)
            addStringProperty("stationCode", station.stationCode)
            addBooleanProperty("selected", station == selected)
        }
    }
)

private fun MapLine.lineFeatures(): FeatureCollection = FeatureCollection.fromFeatures(
    if (stations.size < 2) {
        emptyList()
    } else {
        listOf(
            Feature.fromGeometry(LineString.fromLngLats(stations.map { Point.fromLngLat(it.longitude, it.latitude) }))
        )
    }
)

private fun Style.addTransportLayers(line: MapLine, selected: LocationEntity?, dark: Boolean) {
    val colour = if (dark) {
        if (line.id == "red") "#EF5350" else "#81C784"
    } else {
        line.color
    }
    val halo = if (dark) "#151A20" else "#FFFFFF"
    addSource(GeoJsonSource("${line.id}-route", line.lineFeatures()))
    addSource(GeoJsonSource("${line.id}-markers", line.markerFeatures(selected)))
    addLayer(
        LineLayer("${line.id}-line", "${line.id}-route").withProperties(
            PropertyFactory.lineColor(colour),
            PropertyFactory.lineWidth(ROUTE_WIDTH)
        )
    )
    addLayer(
        CircleLayer("${line.id}-stations", "${line.id}-markers").withProperties(
            PropertyFactory.circleColor(colour),
            PropertyFactory.circleRadius(
                Expression.switchCase(
                    Expression.get("selected"),
                    Expression.literal(SELECTED_MARKER_RADIUS),
                    Expression.literal(MARKER_RADIUS)
                )
            ),
            PropertyFactory.circleStrokeColor(halo),
            PropertyFactory.circleStrokeWidth(2f)
        )
    )
    addLayer(
        SymbolLayer("${line.id}-labels", "${line.id}-markers").withProperties(
            PropertyFactory.textField(Expression.get("name")),
            PropertyFactory.textSize(LABEL_SIZE),
            PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
            PropertyFactory.textColor(if (dark) "#F5F7F8" else "#17241D"),
            PropertyFactory.textHaloColor(halo),
            PropertyFactory.textHaloWidth(2f),
            PropertyFactory.textOffset(arrayOf(0f, LABEL_OFFSET)),
            PropertyFactory.textAnchor("top"),
            PropertyFactory.textAllowOverlap(false)
        ).apply { minZoom = DEFAULT_LOCATION_ZOOM.toFloat() }
    )
}

private fun Style.updateTransportSources(line: MapLine, selected: LocationEntity?) {
    getSourceAs<GeoJsonSource>("${line.id}-route")?.setGeoJson(line.lineFeatures())
    getSourceAs<GeoJsonSource>("${line.id}-markers")?.setGeoJson(line.markerFeatures(selected))
}

@SuppressLint("MissingPermission")
private fun MapLibreMap.updateLocation(style: Style, context: Context, granted: Boolean, followUser: Boolean = true) {
    try {
        if (granted && !locationComponent.isLocationComponentActivated) {
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style).build()
            )
            locationComponent.renderMode = RenderMode.COMPASS
            locationComponent.cameraMode = if (followUser) CameraMode.TRACKING else CameraMode.NONE
        }
        if (locationComponent.isLocationComponentActivated) locationComponent.isLocationComponentEnabled = granted
    } catch (error: SecurityException) {
        Timber.e(error, "Failed to update map location component")
    }
}
