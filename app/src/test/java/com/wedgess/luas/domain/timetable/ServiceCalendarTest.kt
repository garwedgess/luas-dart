package com.wedgess.luas.domain.timetable

import com.wedgess.luas.data.timetable.GtfsCalendar
import com.wedgess.luas.data.timetable.GtfsException
import com.wedgess.luas.data.timetable.GtfsServices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ServiceCalendarTest {
    private val monday = LocalDate.of(2026, 11, 9)
    private val weekly = GtfsCalendar("weekly", 31, monday, monday.plusDays(6))

    @Test
    fun `weekdays bounds and exception precedence are respected`() {
        val rules = GtfsServices(
            listOf(weekly),
            listOf(
                GtfsException("weekly", monday, false),
                GtfsException("special", monday, true)
            )
        )
        assertEquals(setOf("special"), rules.active(monday))
        assertEquals(setOf("weekly"), rules.active(monday.plusDays(1)))
        assertEquals(emptySet<String>(), rules.active(monday.plusDays(5)))
        assertEquals(emptySet<String>(), rules.active(monday.plusDays(7)))
        assertFalse(rules.covers(monday.plusDays(7)))
    }

    @Test
    fun `calendar dates can publish a day beyond the weekly range`() {
        val date = monday.plusDays(10)
        val rules = GtfsServices(listOf(weekly), listOf(GtfsException("weekly", date, true)))
        assertEquals(setOf("weekly"), rules.active(date))
    }

    @Test
    fun `GTFS spring transition uses noon minus twelve hours`() {
        assertEquals(
            Instant.parse("2026-03-29T00:30:00Z"),
            scheduledInstant(
                LocalDate.of(2026, 3, 29),
                5_400,
                ZoneId.of("Europe/Dublin")
            )
        )
    }

    @Test
    fun `GTFS autumn transition is unambiguous`() {
        assertEquals(
            Instant.parse("2026-10-25T01:30:00Z"),
            scheduledInstant(
                LocalDate.of(2026, 10, 25),
                5_400,
                ZoneId.of("Europe/Dublin")
            )
        )
    }
}
