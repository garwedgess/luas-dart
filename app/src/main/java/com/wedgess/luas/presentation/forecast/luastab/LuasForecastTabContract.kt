package com.wedgess.luas.presentation.forecast.luastab

import com.wedgess.luas.domain.model.LuasForcastEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.presentation.forecast.luastab.model.ForecastTabDialogState
import com.wedgess.luas.presentation.forecast.luastab.model.NotificationState

interface LuasForecastTabContract {

    data class UiState(
        val stops: List<LuasStopEntity> = emptyList(),
        val selectedStop: LuasStopEntity = LuasStopEntity.initial(),
        val forecast: LuasForcastEntity = LuasForcastEntity.initial(),
        val refreshProgress: Float = 0f,
        val line: LuasLineEntity = LuasLineEntity.RED,
        val dialog: ForecastTabDialogState = ForecastTabDialogState.None,
        val notificationState: NotificationState = NotificationState(),
        val alarmIsRunning: Boolean = false
    )

    sealed interface Event {
        data class OnStopSelected(val stopAbrv: LuasStopEntity) : Event
        data object OnShowTravelUpdatesDialog : Event
        data class OnNotificationMinutesChanged(val minutes: Int) : Event
        data class OnShowNotificationsDialog(val dueInMins: Int, val destination: String) : Event
        data class OnStartNotification(val minutes: Int) : Event
        data object OnStopNotification : Event
        data object OnDismissDialog : Event
        data object OnRefresh : Event
    }
}
