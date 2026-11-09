package com.wedgess.luas.data.mapper

import com.wedgess.luas.data.model.DartDirectionData
import com.wedgess.luas.data.model.DartStationForecastResponseData
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private const val SECONDS_PER_MINUTE = 60L
private val TRAIN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

fun DartStationForecastResponseData.toDepartures(fetchedAt: Instant): Departures {
    val first = stationData.firstOrNull()
    return Departures(
        stationCode = first?.stationCode?.trim().orEmpty(),
        stationName = first?.stationFullName.orEmpty(),
        fetchedAt = fetchedAt,
        message = "",
        items = stationData.map { data ->
            val expectedAt = fetchedAt.plusSeconds(data.dueIn * SECONDS_PER_MINUTE)
            Departure(
                transport = TransportType.DART,
                direction = data.direction.toDirection(),
                destination = data.destination,
                dueMins = data.dueIn,
                expectedAt = expectedAt,
                // The API reports lateness in minutes against the timetable, so scheduled = expected - late.
                scheduledAt = expectedAt.minusSeconds(data.late * SECONDS_PER_MINUTE),
                lateMins = data.late,
                // The API pads train codes with whitespace (e.g. "E846 ").
                tripCode = data.trainCode.trim(),
                operatingDate = data.trainDate.toOperatingDate(),
                status = data.status,
                lastLocation = data.lastLocation
            )
        }
    )
}

private fun DartDirectionData.toDirection() = when (this) {
    DartDirectionData.NORTHBOUND -> DepartureDirection.NORTHBOUND
    DartDirectionData.SOUTHBOUND -> DepartureDirection.SOUTHBOUND
    DartDirectionData.UNKNOWN -> DepartureDirection.UNKNOWN
}

private fun String.toOperatingDate(): LocalDate? = try {
    LocalDate.parse(trim(), TRAIN_DATE_FORMAT)
} catch (_: DateTimeParseException) {
    null
}
