package com.wedgess.luas.presentation.map.compose

import androidx.compose.runtime.Composable
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.components.EmptyContent
import com.wedgess.luas.presentation.components.ErrorContent
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.map.MapContract
import com.wedgess.luas.presentation.map.compose.components.DartMapLibreMap
import com.wedgess.luas.presentation.map.compose.components.LuasMapLibreMap
import com.wedgess.luas.presentation.map.compose.components.MapDialogs
import com.wedgess.luas.presentation.model.Compose
import com.wedgess.luas.presentation.model.Permission
import com.wedgess.luas.presentation.model.UiResult

@Composable
fun MapScreen(uiResult: UiResult<MapContract.UiState>, onEvent: (MapContract.Event) -> Unit) {
    uiResult.Compose(
        onLoading = { LoadingContent(title = "Loading map info...") },
        onError = { ErrorContent(title = "Failed to load map info: $it") },
        onEmpty = { EmptyContent(title = "No map info...") },
        onSuccess = { uiState ->
            when (uiState.transportType) {
                TransportType.LUAS -> LuasMapLibreMap(
                    currentLocation = uiState.currentLocation,
                    greenLineLocations = uiState.greenLineLocations,
                    redLineLocations = uiState.redLineLocations,
                    locationPermissionGranted = uiState.locationPermission == Permission.Granted,
                )

                TransportType.DART -> DartMapLibreMap(
                    currentLocation = uiState.currentLocation,
                    dartStationLocations = uiState.dartLocations,
                    locationPermissionGranted = uiState.locationPermission == Permission.Granted,
                )
            }

            MapDialogs(
                dialogState = uiState.dialogState,
                onEvent = onEvent,
            )
        },
    )
}
