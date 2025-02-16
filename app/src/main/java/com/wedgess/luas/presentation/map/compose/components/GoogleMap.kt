package com.wedgess.luas.presentation.map.compose.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.wedgess.luas.presentation.map.MapContract

@Composable
fun ComposeGoogleMap(uiState: MapContract.UiState) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(uiState.currentLocation.run {
                LatLng(
                    this.latitude,
                    this.longitude
                )
            }, 12f)
        },
        uiSettings = MapUiSettings(
            compassEnabled = true,
            zoomControlsEnabled = true,
            zoomGesturesEnabled = true,
            myLocationButtonEnabled = true,
            rotationGesturesEnabled = true
        )
    ) {
        uiState.currentLocation.run {
            Marker(
                state = MarkerState(position = LatLng(this.latitude, this.longitude)),
                title = "Current Location",
                snippet = "You are here!",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
            )
        }
        uiState.redLineLocations.forEachIndexed { index, stopEntity ->
            val position = LatLng(stopEntity.latitude, stopEntity.longitude)
            Marker(
                state = MarkerState(position),
                title = stopEntity.name,
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )

            if (index < uiState.redLineLocations.size - 1) {
                val nextPosition = LatLng(
                    uiState.redLineLocations[index + 1].latitude,
                    uiState.redLineLocations[index + 1].longitude
                )
                Polyline(
                    points = listOf(position, nextPosition),
                    color = Color(0xFFE53935),
                    width = 8f
                )
            }
        }
        uiState.greenLineLocations.forEachIndexed { index, stopEntity ->
            val position = LatLng(stopEntity.latitude, stopEntity.longitude)
            Marker(
                state = MarkerState(position),
                title = stopEntity.name,
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            )

            if (index < uiState.greenLineLocations.size - 1) {
                val nextPosition = LatLng(
                    uiState.greenLineLocations[index + 1].latitude,
                    uiState.greenLineLocations[index + 1].longitude
                )
                Polyline(
                    points = listOf(position, nextPosition),
                    color = Color(0xFF66BF63),
                    width = 8f
                )
            }
        }
    }
}