package com.wedgess.luas.presentation.map.model

sealed interface MapDialogState {
    data object None : MapDialogState
    data object LocationPermissionRationale : MapDialogState
    data object LocationPermissionPermanentlyDenied : MapDialogState
}
