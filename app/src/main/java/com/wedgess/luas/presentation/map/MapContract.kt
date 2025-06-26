package com.wedgess.luas.presentation.map

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.model.StationLocationEntity
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.model.UserLocation
import com.wedgess.luas.presentation.map.model.MapDialogState
import com.wedgess.luas.presentation.model.Permission
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.immutableListOf
import kotlinx.collections.immutable.persistentListOf

interface MapContract {

    data class UiState(
        val currentLocation: UserLocation,
        val transportType: TransportType,
        val dartLocations: ImmutableList<StationLocationEntity.DartStationLocationEntity>,
        val greenLineLocations: ImmutableList<StationLocationEntity.LuasStationLocationEntity>,
        val redLineLocations: ImmutableList<StationLocationEntity.LuasStationLocationEntity>,
        val locationPermission: Permission,
        val dialogState: MapDialogState
    ) {
        companion object {
            fun initial() = UiState(
                currentLocation = UserLocation(0.0, 0.0),
                dartLocations = persistentListOf(),
                greenLineLocations = persistentListOf(),
                redLineLocations = persistentListOf(),
                locationPermission = Permission.Unknown,
                transportType = TransportType.LUAS,
                dialogState = MapDialogState.None
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
