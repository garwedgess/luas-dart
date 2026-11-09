package com.wedgess.luas.data.mapper

import com.wedgess.luas.DepartureSnapshot
import com.wedgess.luas.DepartureSnapshotItem
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** [stationCode] is the code the forecast was requested with, so it is also the key used to read it back. */
fun Departures.toSnapshot(transport: TransportType, stationCode: String) = DepartureSnapshot(
    Transport = transport.name,
    StationCode = stationCode,
    StationName = stationName,
    Message = message,
    FetchedAtMillis = fetchedAt.toEpochMilli()
) to items.mapIndexed { index, departure -> departure.toRow(transport, stationCode, index) }

private fun Departure.toRow(transport: TransportType, stationCode: String, position: Int) = DepartureSnapshotItem(
    Transport = transport.name,
    StationCode = stationCode,
    Position = position.toLong(),
    Direction = direction.name,
    Destination = destination,
    DueMins = dueMins?.toLong(),
    ExpectedAtMillis = expectedAt?.toEpochMilli(),
    ScheduledAtMillis = scheduledAt?.toEpochMilli(),
    LateMins = lateMins.toLong(),
    TripCode = tripCode,
    OperatingDate = operatingDate?.toString(),
    Status = status,
    LastLocation = lastLocation
)

fun DepartureSnapshot.toDepartures(items: List<DepartureSnapshotItem>) = Departures(
    stationCode = StationCode,
    stationName = StationName,
    fetchedAt = Instant.ofEpochMilli(FetchedAtMillis),
    message = Message,
    items = items.map { it.toDeparture() }
)

private fun DepartureSnapshotItem.toDeparture() = Departure(
    transport = TransportType.valueOf(Transport),
    direction = DepartureDirection.entries.firstOrNull { it.name == Direction } ?: DepartureDirection.UNKNOWN,
    destination = Destination,
    dueMins = DueMins?.toInt(),
    expectedAt = ExpectedAtMillis?.let(Instant::ofEpochMilli),
    scheduledAt = ScheduledAtMillis?.let(Instant::ofEpochMilli),
    lateMins = LateMins.toInt(),
    tripCode = TripCode,
    operatingDate = OperatingDate?.let {
        try {
            LocalDate.parse(it)
        } catch (_: DateTimeParseException) {
            null
        }
    },
    status = Status,
    lastLocation = LastLocation
)
