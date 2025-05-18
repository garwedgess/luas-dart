package com.wedgess.luas.presentation.forecast.tab.model

sealed interface ForecastTabDialogState {
    data object None : ForecastTabDialogState
    data object Notification : ForecastTabDialogState
    data object TravelUpdatesAlert : ForecastTabDialogState
}
