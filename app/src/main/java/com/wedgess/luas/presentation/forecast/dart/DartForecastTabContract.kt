package com.wedgess.luas.presentation.forecast.dart

import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmCapabilities
import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.StationPreferences
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionedListState
import com.wedgess.luas.presentation.forecast.dart.model.DartDropdownItem
import com.wedgess.luas.presentation.forecast.dart.model.DartForecastSectionRowData
import com.wedgess.luas.presentation.model.DropdownItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant
import java.time.LocalDate

interface DartForecastTabContract {

    data class UiState(
        val sectionedListState: SectionedListState<DartForecastSectionRowData> = SectionedListState(
            sections = persistentListOf()
        ),
        val stations: ImmutableList<DartStationEntity> = persistentListOf(),
        val selectedStation: DropdownItem = DartDropdownItem.default,
        /** True when the latest refresh failed, so the rows are the last live result and show no countdown. */
        val isStale: Boolean = false,
        val fetchedAt: Instant = Instant.EPOCH,
        val dropdownOptions: ImmutableList<DropdownItem> = persistentListOf(),
        val stationPreferences: StationPreferences = StationPreferences(),
        val alarmDraft: AlarmDraft? = null,
        val activeAlarms: ImmutableList<Alarm> = persistentListOf(),
        val alarmCapabilities: AlarmCapabilities = AlarmCapabilities(exactAlarms = true, notifications = true)
    )

    /** The train alarm being set up. [requestId] is fixed when the dialog opens so confirming twice is a no-op. */
    data class AlarmDraft(
        val requestId: String,
        val tripCode: String,
        val operatingDate: LocalDate?,
        val destination: String,
        val direction: DepartureDirection,
        val stationName: String,
        val stationCode: String,
        val dueInMins: Int,
        val expectedAt: Instant,
        val observedAt: Instant,
        val notifyMinutesBefore: Int
    )

    sealed interface Event {
        data class OnStationSelected(val stationName: String) : Event
        data class OnToggleFavourite(val stationName: String, val favourite: Boolean) : Event
        data object OnRefresh : Event
        data class OnToggleSection(val id: Long) : Event
        data class OnShowAlarmDialog(
            val tripCode: String,
            val operatingDate: LocalDate?,
            val dueInMins: Int,
            val destination: String,
            val direction: DepartureDirection
        ) : Event
        data class OnAlarmMinutesChanged(val minutes: Int) : Event
        data class OnStartAlarm(val minutes: Int) : Event
        data class OnCancelAlarm(val alarmId: String) : Event
        data object OnDismissAlarmDialog : Event
        data object OnOpenExactAlarmSettings : Event
    }

    companion object {
        const val REFRESH_INTERVAL_MILLIS = 40_000L
    }
}
