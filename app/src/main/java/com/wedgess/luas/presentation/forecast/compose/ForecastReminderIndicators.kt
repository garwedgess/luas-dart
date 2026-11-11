package com.wedgess.luas.presentation.forecast.compose

import com.wedgess.luas.domain.alarm.AlarmMatch
import com.wedgess.luas.domain.alarm.matchAlarm
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.forecast.dart.model.DartForecastSectionRowData
import com.wedgess.luas.presentation.navigation.reminder.ReminderTarget
import java.time.Instant

/** A destination alone cannot identify a Luas tram; use the alarm coordinator's arrival-window matching. */
internal fun luasDeparturesWithReminders(alarms: List<Alarm>, forecast: Departures): Set<Departure> =
    alarms.filter { it.isActiveAt(TransportType.LUAS, forecast.stationCode) }.mapNotNull { alarm ->
        (matchAlarm(alarm, forecast, Instant.now()) as? AlarmMatch.Found)
            ?.takeUnless { it.ambiguous }?.departure
    }.toSet()

internal fun DartForecastSectionRowData.hasActiveReminder(alarms: List<Alarm>, stationCode: String?): Boolean =
    alarms.any { it.isActiveAt(TransportType.DART, stationCode) && ReminderTarget("", it).matches(this) }

private fun Alarm.isActiveAt(transport: TransportType, stationCode: String?): Boolean =
    state == AlarmState.ACTIVE && this.transport == transport && this.stationCode.equals(stationCode, ignoreCase = true)
