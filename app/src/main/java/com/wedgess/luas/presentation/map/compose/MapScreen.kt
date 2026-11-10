package com.wedgess.luas.presentation.map.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.components.EmptyContent
import com.wedgess.luas.presentation.components.ErrorContent
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.map.MapContract
import com.wedgess.luas.presentation.map.compose.components.DartMapLibreMap
import com.wedgess.luas.presentation.map.compose.components.LuasMapLibreMap
import com.wedgess.luas.presentation.map.compose.components.MapDialogs
import com.wedgess.luas.presentation.map.compose.components.MapStationPicker
import com.wedgess.luas.presentation.map.compose.components.MapStationSheet
import com.wedgess.luas.presentation.map.model.MapStationState
import com.wedgess.luas.presentation.model.Compose
import com.wedgess.luas.presentation.model.Permission
import com.wedgess.luas.presentation.model.UiResult

@Suppress("LongParameterList") // Stateless screen callbacks.
@Composable
fun MapScreen(
    uiResult: UiResult<MapContract.UiState>,
    onEvent: (MapContract.Event) -> Unit,
    stationState: MapStationState?,
    onStationSelect: (LocationEntity) -> Unit,
    onDismissStation: () -> Unit,
    onRetryDepartures: () -> Unit,
    modifier: Modifier = Modifier
) {
    uiResult.Compose(
        onLoading = { LoadingContent(title = "Loading map info...") },
        onError = { ErrorContent(title = "Failed to load map info: $it") },
        onOffline = { ErrorContent(title = "Failed to load map info: $it") },
        onEmpty = { EmptyContent(title = "No map info...") },
        onSuccess = { uiState ->
            Box(modifier = modifier) {
                key(uiState.transportType) {
                    when (uiState.transportType) {
                        TransportType.LUAS -> LuasMapLibreMap(
                            currentLocation = uiState.currentLocation,
                            greenLineLocations = uiState.greenLineLocations,
                            redLineLocations = uiState.redLineLocations,
                            locationPermissionGranted = uiState.locationPermission == Permission.Granted,
                            selectedStation = stationState?.station,
                            onStationSelect = onStationSelect
                        )
                        TransportType.DART -> DartMapLibreMap(
                            currentLocation = uiState.currentLocation,
                            dartStationLocations = uiState.dartLocations,
                            locationPermissionGranted = uiState.locationPermission == Permission.Granted,
                            selectedStation = stationState?.station,
                            onStationSelect = onStationSelect
                        )
                    }
                }
                MapStationPicker(
                    stations = uiState.dartLocations + uiState.greenLineLocations + uiState.redLineLocations,
                    onSelect = onStationSelect,
                    modifier = Modifier.padding(16.dp)
                )
            }
            stationState?.let {
                MapStationSheet(it, onDismissStation, onRetryDepartures)
            }

            MapDialogs(
                dialogState = uiState.dialogState,
                onEvent = onEvent
            )
        }
    )
}
