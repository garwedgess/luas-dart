package com.wedgess.luas.presentation.map.model

import com.wedgess.luas.presentation.model.Permission

data class MapLocationPermissionState(
    val locationPermissionStatus: Permission = Permission.Unknown,
    val dialogState: MapDialogState = MapDialogState.None,
    val locationPermissionPreviouslyGranted: Boolean = false
)
