package com.wedgess.luas.presentation.map

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.domain.model.UserLocation
import com.wedgess.luas.presentation.map.model.MapDialogState
import com.wedgess.luas.presentation.model.Permission

interface MapContract {

    data class UiState(
        val currentLocation: UserLocation,
        val greenLineLocations: List<StopEntity>,
        val redLineLocations: List<StopEntity>,
        val locationPermission: Permission,
        val dialogState: MapDialogState,
    ) {
        companion object {
            fun initial() = UiState(
                currentLocation = UserLocation(0.0, 0.0),
                greenLineLocations = emptyList(),
                redLineLocations = emptyList(),
                locationPermission = Permission.Unknown,
                dialogState = MapDialogState.None,
            )
        }
    }

    sealed interface Event {
        data object OnLocationPermanentlyDeniedDialog : Event
        data object OnAcceptPermissionClick : Event
        data object OnDismissPermissionClick : Event
        data object OnLocationWasRequested : Event
        data object OnDismissDialogClick : Event
        data object OnIgnoreLocationPermissionClick : Event
        data object OnOpenAppSettingsPermissionClick : Event

        @OptIn(ExperimentalPermissionsApi::class)
        data class OnPermissionStateChanged(val state: MultiplePermissionsState) : Event
    }

    sealed interface Effect {
        data object ShowSystemLocationPermissionDialog : Effect
        data object OpenAppPermissionScreen : Effect
    }
}
