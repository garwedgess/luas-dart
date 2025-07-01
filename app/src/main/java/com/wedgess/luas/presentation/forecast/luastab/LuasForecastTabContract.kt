package com.wedgess.luas.presentation.forecast.luastab

import com.wedgess.luas.domain.model.LuasForecastEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.presentation.forecast.luastab.model.LuasForecastTabDialogState
import com.wedgess.luas.presentation.forecast.luastab.model.LuasNotificationState
import com.wedgess.luas.presentation.model.DropdownItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

interface LuasForecastTabContract {

    data class UiState(
        val stops: ImmutableList<LuasStopEntity> = persistentListOf(),
        val selectedStop: DropdownItem = DropdownItem(text = ""),
        val forecast: LuasForecastEntity = LuasForecastEntity.initial(),
        val refreshProgress: Float = 0f,
        val line: LuasLineEntity = LuasLineEntity.RED,
        val dropdownOptions: ImmutableList<DropdownItem> = persistentListOf(),
        val dialog: LuasForecastTabDialogState = LuasForecastTabDialogState.None,
        val luasNotificationState: LuasNotificationState = LuasNotificationState(),
        val alarmIsRunning: Boolean = false
    )

    sealed interface Event {
        data class OnStopSelected(val stopAbrv: String) : Event
        data object OnShowTravelUpdatesDialog : Event
        data class OnNotificationMinutesChanged(val minutes: Int) : Event
        data class OnShowNotificationsDialog(val dueInMins: Int, val destination: String) : Event
        data class OnStartNotification(val minutes: Int) : Event
        data object OnStopNotification : Event
        data object OnDismissDialog : Event
        data object OnRefresh : Event
    }
}
