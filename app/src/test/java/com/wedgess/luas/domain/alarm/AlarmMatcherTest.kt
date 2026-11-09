package com.wedgess.luas.domain.alarm

import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class AlarmMatcherTest {

    private val now = Instant.parse("2026-11-09T22:00:00Z")
    private val date = LocalDate.of(2026, 11, 9)

    private fun minutes(n: Long) = now.plus(Duration.ofMinutes(n))

    private fun luasDeparture(
        inMinutes: Long,
        destination: String = "Broombridge",
        direction: DepartureDirection = DepartureDirection.INBOUND
    ) = Departure(TransportType.LUAS, direction, destination, inMinutes.toInt(), minutes(inMinutes))

    private fun dartDeparture(trip: String, inMinutes: Long, operatingDate: LocalDate? = date) = Departure(
        transport = TransportType.DART,
        direction = DepartureDirection.NORTHBOUND,
        destination = "Malahide",
        dueMins = inMinutes.toInt(),
        expectedAt = minutes(inMinutes),
        tripCode = trip,
        operatingDate = operatingDate
    )

    private fun live(vararg items: Departure) = Departures("X", "X", now, "", items.toList())

    private fun alarm(
        transport: TransportType = TransportType.LUAS,
        kind: AlarmKind = AlarmKind.SPECIFIC_DEPARTURE,
        expectedInMinutes: Long = 20,
        observedAt: Instant = now,
        trip: String? = null,
        operatingDate: LocalDate? = null,
        direction: DepartureDirection? = DepartureDirection.INBOUND,
        destination: String? = "Broombridge",
        lead: Int = 5
    ) = Alarm(
        id = "a",
        requestCode = 1,
        kind = kind,
        state = AlarmState.ACTIVE,
        transport = transport,
        stationCode = "X",
        stationName = "X",
        direction = direction,
        destination = destination,
        notifyMinutesBefore = lead,
        tripCode = trip,
        operatingDate = operatingDate,
        targetExpectedAt = minutes(expectedInMinutes),
        targetObservedAt = observedAt,
        reminderAt = minutes(expectedInMinutes - lead),
        createdAt = observedAt,
        updatedAt = observedAt
    )

    // ---- DART: identity is train code + operating date, never time ----

    @Test
    fun `a DART alarm matches its train by code even when it is much later than first expected`() {
        val result = matchAlarm(
            alarm(TransportType.DART, trip = "E846", operatingDate = date, direction = null, destination = null),
            live(dartDeparture("E999", 10), dartDeparture("E846", 45)),
            now
        )

        assertEquals("E846", (result as AlarmMatch.Found).departure.tripCode)
        assertFalse(result.ambiguous)
    }

    @Test
    fun `a DART alarm does not match the same train code on a different operating date`() {
        val result = matchAlarm(
            alarm(TransportType.DART, trip = "E846", operatingDate = date, direction = null, destination = null),
            live(dartDeparture("E846", 20, operatingDate = date.plusDays(1))),
            now
        )

        assertEquals(AlarmMatch.NotFound, result)
    }

    @Test
    fun `an unknown operating date on either side does not stop a DART match`() {
        val unknownLive = matchAlarm(
            alarm(TransportType.DART, trip = "E846", operatingDate = date, direction = null, destination = null),
            live(dartDeparture("E846", 20, operatingDate = null)),
            now
        )
        val unknownAlarm = matchAlarm(
            alarm(TransportType.DART, trip = "E846", operatingDate = null, direction = null, destination = null),
            live(dartDeparture("E846", 20)),
            now
        )

        assertTrue(unknownLive is AlarmMatch.Found)
        assertTrue(unknownAlarm is AlarmMatch.Found)
    }

    @Test
    fun `a DART alarm without a train code never matches`() {
        val result = matchAlarm(
            alarm(TransportType.DART, trip = null, direction = null, destination = null),
            live(dartDeparture("E846", 20)),
            now
        )

        assertEquals(AlarmMatch.NotFound, result)
    }

    // ---- Luas: no vehicle identity, so station/direction/destination plus an arrival window ----

    @Test
    fun `a Luas tram is matched by destination and direction inside the arrival window`() {
        val result = matchAlarm(alarm(), live(luasDeparture(21)), now)

        assertEquals(minutes(21), (result as AlarmMatch.Found).departure.expectedAt)
        assertFalse(result.ambiguous)
    }

    @Test
    fun `a tram with the same destination outside the window is not treated as the same tram`() {
        val result = matchAlarm(alarm(), live(luasDeparture(30)), now)

        assertEquals(AlarmMatch.NotFound, result)
    }

    @Test
    fun `a different destination or direction never matches even at the same time`() {
        val wrongDestination = matchAlarm(alarm(), live(luasDeparture(20, "Parnell")), now)
        val wrongDirection = matchAlarm(
            alarm(),
            live(luasDeparture(20, direction = DepartureDirection.OUTBOUND)),
            now
        )

        assertEquals(AlarmMatch.NotFound, wrongDestination)
        assertEquals(AlarmMatch.NotFound, wrongDirection)
    }

    @Test
    fun `destination matching ignores case`() {
        val result = matchAlarm(alarm(), live(luasDeparture(20, "BROOMBRIDGE")), now)

        assertTrue(result is AlarmMatch.Found)
    }

    @Test
    fun `two trams inside the window are ambiguous and the earlier one is used`() {
        val result = matchAlarm(
            alarm(),
            live(luasDeparture(22), luasDeparture(19)),
            now
        )

        val found = result as AlarmMatch.Found
        assertTrue("never silently pick one as if it were certain", found.ambiguous)
        assertEquals(minutes(19), found.departure.expectedAt)
    }

    @Test
    fun `the window widens with the time since the tram was observed`() {
        val observedLongAgo = alarm(observedAt = now.minus(Duration.ofMinutes(20)))

        // 6 minutes late is outside the base window but inside the window after 20 minutes of drift.
        assertTrue(matchAlarm(observedLongAgo, live(luasDeparture(26)), now) is AlarmMatch.Found)
        assertEquals(
            AlarmMatch.NotFound,
            matchAlarm(alarm(observedAt = now), live(luasDeparture(26)), now)
        )
    }

    @Test
    fun `the Luas window is capped`() {
        assertEquals(Duration.ofMinutes(3), luasWindow(Duration.ZERO))
        assertEquals(Duration.ofMinutes(8), luasWindow(Duration.ofHours(5)))
        assertEquals(Duration.ofMinutes(3), luasWindow(Duration.ofMinutes(-10)))
    }

    @Test
    fun `a Luas alarm with no observed target cannot match`() {
        assertEquals(
            AlarmMatch.NotFound,
            matchAlarm(alarm().copy(targetExpectedAt = null), live(luasDeparture(20)), now)
        )
    }

    @Test
    fun `departures of another transport are ignored`() {
        val result = matchAlarm(alarm(), live(dartDeparture("E1", 20)), now)

        assertEquals(AlarmMatch.NotFound, result)
        assertNull((result as? AlarmMatch.Found))
    }

    // ---- Next-departure alarms follow the next qualifying service ----

    @Test
    fun `a next-departure alarm follows the earliest service it can still notify in time for`() {
        val nextAlarm = alarm(kind = AlarmKind.NEXT_DEPARTURE, lead = 5)

        val result = matchAlarm(
            nextAlarm,
            live(luasDeparture(3), luasDeparture(12), luasDeparture(30)),
            now
        )

        // The 3 minute tram is too close for a 5 minute reminder, so it moves on to the next one.
        assertEquals(minutes(12), (result as AlarmMatch.Found).departure.expectedAt)
    }

    @Test
    fun `a next-departure alarm respects its direction and destination filters`() {
        val nextAlarm = alarm(kind = AlarmKind.NEXT_DEPARTURE, destination = null)

        val result = matchAlarm(
            nextAlarm,
            live(luasDeparture(10, direction = DepartureDirection.OUTBOUND), luasDeparture(15)),
            now
        )

        assertEquals(minutes(15), (result as AlarmMatch.Found).departure.expectedAt)
    }

    @Test
    fun `a next-departure alarm finds nothing when no service qualifies`() {
        val result = matchAlarm(alarm(kind = AlarmKind.NEXT_DEPARTURE), live(), now)

        assertEquals(AlarmMatch.NotFound, result)
    }
}
