package com.wedgess.luas.presentation.forecast.model

sealed interface ForecastDialogState {
    data object None : ForecastDialogState
    data object TravelUpdatesAlert : ForecastDialogState
}
