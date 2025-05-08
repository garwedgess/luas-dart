package com.wedgess.luas.presentation.model

sealed class Permission {
    data object Granted : Permission()
    data object Denied : Permission()
    data object PermanentlyDenied : Permission()
    data object ShowRationale : Permission()
    data object Unknown : Permission()
}
