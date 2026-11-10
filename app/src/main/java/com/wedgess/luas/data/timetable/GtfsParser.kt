package com.wedgess.luas.data.timetable

import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.timetable.TimetableStation
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream
import java.util.zip.ZipFile
import javax.inject.Inject

private const val MAX_TEXT_BYTES = 16L * 1024 * 1024
private const val MAX_ROWS = 250_000
private const val MINUTES_PER_HOUR = 60L
private const val SECONDS_PER_HOUR = 3600L
private const val TIME_FIELD_COUNT = 3
private const val MAX_GTFS_HOURS = 167L

class GtfsParser @Inject constructor() {
    fun parse(file: File, transport: TransportType): GtfsFeed = ZipFile(file).use { zip ->
        val routes = readRoutes(zip, transport)
        require(routes.isNotEmpty()) { "No supported routes in feed" }
        val trips = readTrips(zip, transport, routes)
        require(trips.isNotEmpty() && trips.map { it.id }.distinct().size == trips.size) { "Invalid GTFS trips" }
        val tripIds = trips.mapTo(hashSetOf()) { it.id }
        require(zip.rows("frequencies.txt", optional = true).none { it["trip_id"] in tripIds }) {
            "Frequency-based timetables are not supported"
        }
        val stops = zip.rows("stops.txt").associate { row ->
            val name = StationNames.canonical(transport, row.required("stop_name"))
            row.required("stop_id") to TimetableStation(StationNames.key(name), name)
        }
        val times = readTimes(zip, tripIds, stops)
        require(times.isNotEmpty()) { "No explicit scheduled departures" }
        val services = readServices(zip, trips.mapTo(hashSetOf()) { it.serviceId })
        require(services.dates.isNotEmpty()) { "No service calendars" }
        require(
            trips.all { trip ->
                services.calendars.any { it.id == trip.serviceId } ||
                    services.exceptions.any { it.serviceId == trip.serviceId }
            }
        ) { "Trip has no service calendar" }
        val zones = zip.rows("agency.txt").map { ZoneId.of(it.required("agency_timezone")) }.distinct()
        require(zones.size == 1) { "Unsupported agency time zones" }
        val usedStations = times.mapTo(hashSetOf()) { it.stationId }
        GtfsFeed(
            transport,
            zones.single(),
            zip.rows("feed_info.txt", optional = true).firstOrNull()?.get("feed_version").orEmpty(),
            GtfsSchedule(stops.values.distinctBy { it.id }.filter { it.id in usedStations }, trips, times, services)
        )
    }

    private fun readRoutes(zip: ZipFile, transport: TransportType): Map<String, String> {
        return zip.rows("routes.txt").filter { row ->
            if (transport == TransportType.LUAS) row["route_type"] == "0" else row["route_short_name"] == "DART"
        }.associate { it.required("route_id") to it.required("route_short_name") }
    }

    private fun readTrips(zip: ZipFile, transport: TransportType, routes: Map<String, String>): List<GtfsTrip> {
        return zip.rows("trips.txt").filter { it["route_id"] in routes }.map { row ->
            val direction = row["direction_id"]
            GtfsTrip(
                row.required("trip_id"),
                row.required("service_id"),
                StationNames.canonical(transport, row.required("trip_headsign")),
                if (transport == TransportType.LUAS) {
                    when (direction) {
                        "0" -> "Outbound"
                        "1" -> "Inbound"
                        else -> "Direction not specified"
                    }
                } else {
                    when (direction) {
                        "0" -> "Northbound"
                        "1" -> "Southbound"
                        else -> "Direction not specified"
                    }
                },
                routes.getValue(row.required("route_id"))
            )
        }
    }

    private fun readTimes(zip: ZipFile, trips: Set<String>, stops: Map<String, TimetableStation>): List<GtfsTime> {
        val times = mutableListOf<GtfsTime>()
        zip.read("stop_times.txt") { row ->
            if (row["trip_id"] in trips && row["pickup_type"].orEmpty() in setOf("", "0")) {
                val value = row["departure_time"].orEmpty()
                if (value.isNotBlank()) {
                    val stop = stops[row.required("stop_id")] ?: error("Unknown GTFS stop")
                    times.add(
                        GtfsTime(
                            row.required("trip_id"),
                            row.required("stop_sequence").toLong(),
                            stop.id,
                            parseGtfsTime(value),
                            row["stop_headsign"].orEmpty()
                        )
                    )
                }
            }
        }
        require(times.map { it.tripId to it.sequence }.distinct().size == times.size) { "Duplicate GTFS stop sequence" }
        return times
    }

    private fun readServices(zip: ZipFile, used: Set<String>): GtfsServices {
        val weekdays = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        val calendars = zip.rows("calendar.txt", optional = true).filter { it["service_id"] in used }.map { row ->
            val mask = weekdays.withIndex().sumOf { (index, day) ->
                val enabled = row.required(day)
                require(enabled == "0" || enabled == "1")
                if (enabled == "1") 1 shl index else 0
            }
            val start = gtfsDate(row.required("start_date"))
            val end = gtfsDate(row.required("end_date"))
            require(start <= end) { "Invalid service date range" }
            GtfsCalendar(row.required("service_id"), mask, start, end)
        }
        val exceptions = zip.rows(
            "calendar_dates.txt",
            optional = true
        ).filter { it["service_id"] in used }.map { row ->
            val type = row.required("exception_type")
            require(type == "1" || type == "2")
            GtfsException(row.required("service_id"), gtfsDate(row.required("date")), type == "1")
        }
        require(
            exceptions.distinctBy { it.serviceId to it.date }.size == exceptions.size
        ) { "Duplicate service exception" }
        require(calendars.distinctBy { it.id }.size == calendars.size) { "Duplicate service calendar" }
        return GtfsServices(calendars, exceptions)
    }
}

internal fun parseGtfsTime(value: String): Long {
    val parts = value.split(':').map { it.toLong() }
    require(
        parts.size == TIME_FIELD_COUNT && parts[0] in 0..MAX_GTFS_HOURS && parts[1] in 0 until MINUTES_PER_HOUR &&
            parts[2] in 0 until MINUTES_PER_HOUR
    ) { "Invalid GTFS time" }
    return parts[0] * SECONDS_PER_HOUR + parts[1] * MINUTES_PER_HOUR + parts[2]
}

private fun gtfsDate(value: String): LocalDate = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)

private fun ZipFile.rows(name: String, optional: Boolean = false): List<Map<String, String>> =
    mutableListOf<Map<String, String>>().also { rows -> read(name, optional) { rows.add(it) } }

private fun ZipFile.read(name: String, optional: Boolean = false, action: (Map<String, String>) -> Unit) {
    val entry = getEntry(name)
    if (entry == null && optional) return
    requireNotNull(entry) { "GTFS file missing: $name" }
    require(entry.size in 0..MAX_TEXT_BYTES) { "GTFS text file too large: $name" }
    val crc = CRC32()
    CheckedInputStream(getInputStream(entry), crc).bufferedReader(Charsets.UTF_8).use { reader ->
        // Strip the optional BOM before the CSV state machine sees the first quote.
        reader.mark(1)
        if (reader.read() != '\uFEFF'.code) reader.reset()
        var count = 0
        GtfsCsv(reader).forEach {
            require(++count <= MAX_ROWS) { "Too many GTFS records" }
            action(it)
        }
    }
    require(crc.value == entry.crc) { "Corrupt GTFS entry: $name" }
}
