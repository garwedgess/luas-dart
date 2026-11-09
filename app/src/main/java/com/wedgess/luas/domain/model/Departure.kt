package com.wedgess.luas.domain.model

import com.wedgess.luas.domain.Polled
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

enum class DepartureDirection(val key: String) {
    INBOUND("Inbound"),
    OUTBOUND("Outbound"),
    NORTHBOUND("Northbound"),
    SOUTHBOUND("Southbound"),
    UNKNOWN("Unknown");

    /** Stable section identifier for list UIs. */
    val id: Long get() = ordinal + 1L
}

/**
 * A single predicted departure as reported by a live API at [Departures.fetchedAt].
 *
 * [dueMins] is only meaningful relative to the time it was fetched, so anything older than the
 * fetch must be shown as an absolute [expectedAt] time, never as a ticking countdown.
 */
data class Departure(
    val transport: TransportType,
    val direction: DepartureDirection,
    val destination: String,
    /** Minutes until arrival as reported by the API; null when the API gives a service message instead. */
    val dueMins: Int?,
    /** `fetchedAt + dueMins`; null when [dueMins] is null. */
    val expectedAt: Instant?,
    /** Timetabled time, only known for DART (the Luas API exposes no schedule). */
    val scheduledAt: Instant? = null,
    val lateMins: Int = 0,
    /** DART train code. Null for Luas, whose API exposes no vehicle identity. */
    val tripCode: String? = null,
    /** DART operating date; together with [tripCode] it identifies a DART trip. Null for Luas. */
    val operatingDate: LocalDate? = null,
    val status: String? = null,
    val lastLocation: String? = null
)

data class Departures(
    val stationCode: String,
    val stationName: String,
    /** Device clock time at which the network response was received. */
    val fetchedAt: Instant,
    val message: String,
    val items: List<Departure>
) {
    fun byDirection(direction: DepartureDirection) = items.filter { it.direction == direction }

    /** Drops departures whose expected time has already passed, used when showing data that is no longer live. */
    fun withoutPast(now: Instant) = copy(items = items.filter { it.expectedAt == null || it.expectedAt.isAfter(now) })

    companion object {
        val empty = Departures(
            stationCode = "",
            stationName = "",
            fetchedAt = Instant.EPOCH,
            message = "",
            items = emptyList()
        )
    }
}

/** Data older than this is never shown, even marked as stale, because its times can no longer be trusted. */
val MaxStaleAge: Duration = Duration.ofMinutes(10)

/**
 * The departures that may be shown for this poll outcome, or null when there is nothing trustworthy to show.
 * When the latest attempt failed the data is no longer live, so already-passed departures are dropped.
 */
fun Polled<Departures>.displayable(now: Instant): Departures? {
    val data = data ?: return null
    if (error == null) return data
    return if (data.fetchedAt.plus(MaxStaleAge).isBefore(now)) null else data.withoutPast(now)
}
