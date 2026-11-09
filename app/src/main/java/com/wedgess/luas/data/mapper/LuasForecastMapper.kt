package com.wedgess.luas.data.mapper

import com.wedgess.luas.data.model.LuasDirectionKeyData
import com.wedgess.luas.data.model.LuasStopForcastResponseData
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import java.time.Instant

private const val NO_TRAMS_NEWS_MSG = "See news for information"
private const val NO_TRAMS_MSG = "No trams forecast"
private const val SECONDS_PER_MINUTE = 60L

fun LuasStopForcastResponseData.toDepartures(fetchedAt: Instant) = Departures(
    stationCode = stopAbv,
    stationName = stop,
    fetchedAt = fetchedAt,
    message = message,
    items = direction.flatMap { directionData ->
        val departureDirection = directionData.name.toDirection()
        directionData.tram
            .filterNot { it.destination == NO_TRAMS_MSG || it.destination == NO_TRAMS_NEWS_MSG }
            .map { it.toDeparture(departureDirection, fetchedAt) }
    }
)

private fun LuasDirectionKeyData.toDirection() = when (this) {
    LuasDirectionKeyData.INBOUND -> DepartureDirection.INBOUND
    LuasDirectionKeyData.OUTBOUND -> DepartureDirection.OUTBOUND
}

private fun LuasStopForcastResponseData.DirectionData.TramData.toDeparture(
    direction: DepartureDirection,
    fetchedAt: Instant
): Departure {
    // Service messages ("... service ...") are shown in the tram list but carry no countdown.
    val dueMins = if (destination.contains(" service", ignoreCase = true)) {
        null
    } else {
        dueMins.toIntOrNull() ?: 0
    }
    return Departure(
        transport = TransportType.LUAS,
        direction = direction,
        destination = destination,
        dueMins = dueMins,
        expectedAt = dueMins?.let { fetchedAt.plusSeconds(it * SECONDS_PER_MINUTE) }
    )
}
