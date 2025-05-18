package com.wedgess.luas.presentation.forecast.model

sealed interface ForecastDialogState {
    data object None : ForecastDialogState
    data object ScheduleExactAlarms : ForecastDialogState
    data object NotificationPermissionRationale : ForecastDialogState
    data object NotificationPermissionPermanentlyDenied : ForecastDialogState
}
