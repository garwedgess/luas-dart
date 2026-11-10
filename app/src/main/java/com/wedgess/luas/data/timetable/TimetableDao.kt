package com.wedgess.luas.data.timetable

import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.timetable.SECONDS_PER_DAY
import com.wedgess.luas.domain.timetable.ScheduledDeparture
import com.wedgess.luas.domain.timetable.TimetableDay
import com.wedgess.luas.domain.timetable.TimetableFeed
import com.wedgess.luas.domain.timetable.TimetableStation
import com.wedgess.luas.domain.timetable.TimetableZone
import com.wedgess.luas.domain.timetable.scheduledInstant
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** All methods run on the repository's IO dispatcher. A feed and its data are replaced in one SQL transaction. */
class TimetableDao @Inject constructor(private val db: LuasDatabase) {
    private val queries = db.timetableQueries

    fun feed(transport: TransportType): TimetableFeed? = queries.selectFeed(transport.name).executeAsOneOrNull()?.let {
        TimetableFeed(
            transport, Instant.ofEpochMilli(it.DownloadedAtMillis), Instant.ofEpochMilli(it.CheckedAtMillis),
            LocalDate.ofEpochDay(it.ValidFromDay), LocalDate.ofEpochDay(it.ValidUntilDay), ZoneId.of(it.Zone),
            it.MaxDepartureSeconds, it.ETag, it.LastModified, it.Version
        )
    }

    fun stations(transport: TransportType): List<TimetableStation> =
        queries.selectStations(transport.name).executeAsList().map { TimetableStation(it.StationId, it.Name) }

    fun day(transport: TransportType, stationId: String, date: LocalDate): TimetableDay = db.transactionWithResult {
        val feed = feed(transport) ?: return@transactionWithResult TimetableDay(false, emptyList())
        val services = services(transport)
        // A civil-date browser includes after-midnight trips from earlier service days. The extra day covers DST.
        val lookback = feed.maxDepartureSeconds / SECONDS_PER_DAY + 1
        val candidates = (-lookback..1L).associate { offset ->
            val serviceDate = date.plusDays(offset)
            serviceDate to services.active(serviceDate)
        }
        val start = date.atStartOfDay(TimetableZone).toInstant()
        val end = date.plusDays(1).atStartOfDay(TimetableZone).toInstant()
        val times = queries.selectTimes(transport.name, stationId).executeAsList()
        val departures = candidates.flatMap { (serviceDate, active) ->
            times.filter { it.ServiceId in active }.mapNotNull { row ->
                val instant = scheduledInstant(serviceDate, row.DepartureSeconds, feed.zone)
                if (instant < start || instant >= end) {
                    null
                } else {
                    ScheduledDeparture(
                        row.TripId,
                        row.Headsign.ifBlank { row.Destination },
                        row.Line,
                        row.Direction,
                        instant,
                        serviceDate,
                        row.Sequence
                    )
                }
            }
        }.sortedBy { it.scheduledAt }
        // A final service day can spill into tomorrow; never hide an explicitly published post-midnight trip.
        TimetableDay(services.covers(date) || departures.isNotEmpty(), departures)
    }

    fun replace(parsed: GtfsFeed, feed: TimetableFeed, checkCancelled: () -> Unit = {}) = db.transaction {
        val key = parsed.transport.name
        queries.deleteTimes(key)
        queries.deleteTrips(key)
        queries.deleteStations(key)
        queries.deleteCalendars(key)
        queries.deleteExceptions(key)
        insertSchedule(parsed, checkCancelled)
        queries.insertFeed(
            key, feed.etag, feed.lastModified, feed.downloadedAt.toEpochMilli(), feed.checkedAt.toEpochMilli(),
            feed.validFrom.toEpochDay(),
            feed.validUntil.toEpochDay(),
            feed.zone.id,
            feed.version,
            feed.maxDepartureSeconds
        )
    }

    fun checked(transport: TransportType, now: Instant) = queries.markChecked(now.toEpochMilli(), transport.name)

    private fun services(transport: TransportType): GtfsServices = GtfsServices(
        queries.selectCalendars(transport.name).executeAsList().map {
            GtfsCalendar(
                it.ServiceId,
                it.Weekdays.toInt(),
                LocalDate.ofEpochDay(it.StartDay),
                LocalDate.ofEpochDay(it.EndDay)
            )
        },
        queries.selectExceptions(transport.name).executeAsList().map {
            GtfsException(it.ServiceId, LocalDate.ofEpochDay(it.Day), it.Added == 1L)
        }
    )

    private fun insertSchedule(parsed: GtfsFeed, checkCancelled: () -> Unit) {
        val key = parsed.transport.name
        val data = parsed.schedule
        data.stations.forEach { queries.insertStation(key, it.id, it.name) }
        data.trips.forEach { queries.insertTrip(key, it.id, it.serviceId, it.destination, it.direction, it.line) }
        data.times.forEach {
            checkCancelled()
            queries.insertTime(key, it.tripId, it.sequence, it.stationId, it.seconds, it.headsign)
        }
        data.services.calendars.forEach {
            queries.insertCalendar(key, it.id, it.weekdays.toLong(), it.start.toEpochDay(), it.end.toEpochDay())
        }
        data.services.exceptions.forEach {
            queries.insertException(key, it.serviceId, it.date.toEpochDay(), if (it.added) 1L else 0L)
        }
    }
}
