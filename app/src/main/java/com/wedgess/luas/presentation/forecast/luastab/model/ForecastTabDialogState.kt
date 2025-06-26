package com.wedgess.luas.presentation.forecast.luastab.model

sealed interface ForecastTabDialogState {
    data object None : ForecastTabDialogState
    data object Notification : ForecastTabDialogState
    data object TravelUpdatesAlert : ForecastTabDialogState
}
