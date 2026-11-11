package com.wedgess.luas.presentation.forecast.compose

import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.forecast.dart.model.DartForecastSectionRowData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ForecastReminderIndicatorsTest {
    private val now = Instant.now()
    private val date = LocalDate.of(2026, 11, 11)
    private val tram = Departure(
        TransportType.LUAS,
        DepartureDirection.INBOUND,
        "Broombridge",
        6,
        now.plusSeconds(360)
    )
    private val forecast = Departures(
        "PHI",
        "Phibsborough",
        now,
        "",
        listOf(
            tram,
            tram.copy(
                dueMins = 18,
                expectedAt = now.plusSeconds(1080)
            )
        )
    )
    private val alarm = Alarm(
        id = "a", requestCode = 1, kind = AlarmKind.SPECIFIC_DEPARTURE, state = AlarmState.ACTIVE,
        transport = TransportType.LUAS, stationCode = "PHI", stationName = "Phibsborough",
        direction = DepartureDirection.INBOUND, destination = "Broombridge", notifyMinutesBefore = 3,
        targetExpectedAt = tram.expectedAt, targetObservedAt = now,
        reminderAt = now.plusSeconds(180), createdAt = now, updatedAt = now
    )
    private val train = DartForecastSectionRowData(
        trainCode = "E134",
        destination = "Howth",
        dueIn = 8,
        late = 0,
        expectedAt = "09:14",
        scheduledAt = "09:14",
        direction = DepartureDirection.NORTHBOUND,
        operatingDate = date
    )
    private val dartAlarm = alarm.copy(
        transport = TransportType.DART,
        stationCode = "BYSDE",
        tripCode = "E134",
        direction = DepartureDirection.NORTHBOUND,
        destination = "Howth",
        operatingDate = date
    )

    @Test
    fun `only the matching tram receives a filled icon even when destinations repeat`() {
        assertEquals(setOf(tram), luasDeparturesWithReminders(listOf(alarm), forecast))
    }

    @Test
    fun `other stations and inactive alarms do not mark a tram`() {
        val unrelated = listOf(alarm.copy(stationCode = "BRO"), alarm.copy(state = AlarmState.CANCELLED))
        assertTrue(luasDeparturesWithReminders(unrelated, forecast).isEmpty())
    }

    @Test
    fun `ambiguous trams are not both shown as having the reminder`() {
        val ambiguous = forecast.copy(items = listOf(tram, tram.copy(dueMins = 7, expectedAt = now.plusSeconds(420))))
        assertTrue(luasDeparturesWithReminders(listOf(alarm), ambiguous).isEmpty())
    }

    @Test
    fun `DART indicator matches station train date direction and active status`() {
        assertTrue(train.hasActiveReminder(listOf(dartAlarm), "bysde"))
        val unrelated = listOf(
            dartAlarm.copy(stationCode = "OTHER"),
            dartAlarm.copy(state = AlarmState.FIRED),
            dartAlarm.copy(tripCode = "E135"),
            dartAlarm.copy(operatingDate = date.plusDays(1)),
            dartAlarm.copy(direction = DepartureDirection.SOUTHBOUND)
        )
        assertFalse(train.hasActiveReminder(unrelated, "BYSDE"))
    }
}
