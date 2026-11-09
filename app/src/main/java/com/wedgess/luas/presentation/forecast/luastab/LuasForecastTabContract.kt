package com.wedgess.luas.presentation.forecast.luastab

import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.presentation.forecast.luastab.model.LuasDropdownItem
import com.wedgess.luas.presentation.forecast.luastab.model.LuasForecastTabDialogState
import com.wedgess.luas.presentation.forecast.luastab.model.LuasNotificationState
import com.wedgess.luas.presentation.model.DropdownItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

interface LuasForecastTabContract {

    data class UiState(
        val stops: ImmutableList<LuasStopEntity> = persistentListOf(),
        val selectedStop: DropdownItem = LuasDropdownItem.default,
        val forecast: Departures = Departures.empty,
        /** True when the latest refresh failed, so [forecast] is the last live result and must not be counted down. */
        val isStale: Boolean = false,
        val line: LuasLineEntity = LuasLineEntity.RED,
        val dropdownOptions: ImmutableList<DropdownItem> = persistentListOf(),
        val dialog: LuasForecastTabDialogState = LuasForecastTabDialogState.None,
        val luasNotificationState: LuasNotificationState = LuasNotificationState(),
        val alarmIsRunning: Boolean = false
    )

    sealed interface Event {
        data class OnStopSelected(val stopName: String) : Event
        data object OnShowTravelUpdatesDialog : Event
        data class OnNotificationMinutesChanged(val minutes: Int) : Event
        data class OnShowNotificationsDialog(val dueInMins: Int, val destination: String) : Event
        data class OnStartNotification(val minutes: Int) : Event
        data object OnStopNotification : Event
        data object OnDismissDialog : Event
        data object OnRefresh : Event
    }

    companion object {
        const val REFRESH_INTERVAL_MILLIS = 15_000L
    }
}
