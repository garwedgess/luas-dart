package com.wedgess.luas.presentation.forecast

import com.wedgess.luas.domain.model.ForcastEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.presentation.forecast.model.ForecastDialogState
import com.wedgess.luas.presentation.forecast.model.NotificationState

interface ForecastContract {

    data class UiState(
        val stops: List<StopEntity> = emptyList(),
        val selectedStop: StopEntity = StopEntity.initial(),
        val forecast: ForcastEntity = ForcastEntity.initial(),
        val refreshProgress: Float = 0f,
        val line: LuasLineEntity = LuasLineEntity.RED,
        val dialog: ForecastDialogState = ForecastDialogState.None,
        val notificationState: NotificationState = NotificationState(),
        val alarmIsRunning: Boolean = false
    )

    sealed interface Event {
        data class OnStopSelected(val stopAbrv: StopEntity) : Event
        data object OnShowTravelUpdatesDialog : Event
        data class OnNotificationMinutesChanged(val minutes: Int) : Event
        data class OnShowNotificationsDialog(val dueInMins: Int, val destination: String) : Event
        data class OnStartNotification(val minutes: Int) : Event
        data object OnStopNotification : Event
        data object OnDismissDialog : Event
        data object OnRefresh : Event
    }
}
