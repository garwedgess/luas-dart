package com.wedgess.luas.presentation.forecast.luastab.model

sealed interface LuasForecastTabDialogState {
    data object None : LuasForecastTabDialogState
    data object Notification : LuasForecastTabDialogState
    data object TravelUpdatesAlert : LuasForecastTabDialogState
}
