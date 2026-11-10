package com.wedgess.luas.presentation.navigation.reminder

import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.forecast.dart.model.DartForecastSectionRowData

/** A fresh token for each tap allows repeated taps without replaying a consumed highlight on rotation. */
data class ReminderTarget(val token: String, val alarm: Alarm, val line: LuasLineEntity? = null) {
    fun isForDartStation(stationCode: String?): Boolean =
        alarm.transport == TransportType.DART && alarm.stationCode.equals(stationCode, ignoreCase = true)

    fun matches(row: DartForecastSectionRowData): Boolean =
        alarm.tripCode == row.trainCode &&
            (alarm.operatingDate == null || row.operatingDate == null || alarm.operatingDate == row.operatingDate) &&
            (alarm.direction == null || alarm.direction == row.direction)
}

data class ReminderNavigationState(
    val target: ReminderTarget? = null,
    val loading: Boolean = false,
    val error: ReminderNavigationError? = null
)

enum class ReminderNavigationError { OPEN_FAILED, DEPARTURE_UNAVAILABLE }
