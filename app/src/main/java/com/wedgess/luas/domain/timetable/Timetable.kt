package com.wedgess.luas.domain.timetable

import com.wedgess.luas.domain.model.TransportType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val TimetableZone: ZoneId = ZoneId.of("Europe/Dublin")

/** Scheduled data has a separate model and repository; it cannot be passed to the live alarm coordinator. */
data class ScheduledDeparture(
    val tripId: String,
    val destination: String,
    val line: String,
    val direction: String,
    val scheduledAt: Instant,
    val serviceDate: LocalDate,
    val stopSequence: Long = 0
)

data class TimetableStation(val id: String, val name: String, val lines: Set<String> = emptySet())

data class TimetableFeed(
    val transport: TransportType,
    val downloadedAt: Instant,
    val checkedAt: Instant,
    val validFrom: LocalDate,
    val validUntil: LocalDate,
    val zone: ZoneId,
    val maxDepartureSeconds: Long,
    val etag: String? = null,
    val lastModified: String? = null,
    val version: String = ""
)

data class TimetableDay(val published: Boolean, val departures: List<ScheduledDeparture>)

interface TimetableRepository {
    suspend fun feed(transport: TransportType): TimetableFeed?
    suspend fun stations(transport: TransportType): List<TimetableStation>
    suspend fun day(transport: TransportType, stationId: String, date: LocalDate): TimetableDay

    /** A failed download/import leaves the last complete timetable intact. Updates only run in the foreground. */
    suspend fun refresh(transport: TransportType, force: Boolean = false): Result<Unit>
}

/** GTFS measures a service day from local noon minus 12 hours, including on daylight-saving transitions. */
fun scheduledInstant(serviceDate: LocalDate, seconds: Long, zone: ZoneId): Instant =
    serviceDate.atTime(
        SERVICE_NOON_HOUR,
        0
    ).atZone(zone).toInstant().minusSeconds(HALF_DAY_SECONDS).plusSeconds(seconds)

const val SECONDS_PER_DAY = 86_400L
private const val SERVICE_NOON_HOUR = 12
private const val HALF_DAY_SECONDS = 43_200L
