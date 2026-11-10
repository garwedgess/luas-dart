package com.wedgess.luas.data.timetable

import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.timetable.TimetableStation
import java.time.LocalDate
import java.time.ZoneId

data class GtfsTrip(
    val id: String,
    val serviceId: String,
    val destination: String,
    val direction: String,
    val line: String
)

data class GtfsTime(
    val tripId: String,
    val sequence: Long,
    val stationId: String,
    val seconds: Long,
    val headsign: String
)

data class GtfsCalendar(val id: String, val weekdays: Int, val start: LocalDate, val end: LocalDate) {
    fun runs(date: LocalDate): Boolean = date in start..end && weekdays and (1 shl (date.dayOfWeek.value - 1)) != 0
}

data class GtfsException(val serviceId: String, val date: LocalDate, val added: Boolean)

data class GtfsServices(val calendars: List<GtfsCalendar>, val exceptions: List<GtfsException>) {
    val dates: List<LocalDate> get() = calendars.flatMap { listOf(it.start, it.end) } + exceptions.map { it.date }

    fun active(date: LocalDate): Set<String> {
        val active = calendars.filter { it.runs(date) }.mapTo(mutableSetOf()) { it.id }
        exceptions.filter { it.date == date }.forEach {
            if (it.added) {
                active.add(
                    it.serviceId
                )
            } else {
                active.remove(it.serviceId)
            }
        }
        return active
    }

    // A weekly calendar is authoritative only within its own bounds. No extrapolation from feed_info dates.
    fun covers(
        date: LocalDate
    ): Boolean = calendars.any { date in it.start..it.end } || exceptions.any { it.date == date }
}

data class GtfsSchedule(
    val stations: List<TimetableStation>,
    val trips: List<GtfsTrip>,
    val times: List<GtfsTime>,
    val services: GtfsServices
)

data class GtfsFeed(val transport: TransportType, val zone: ZoneId, val version: String, val schedule: GtfsSchedule)
