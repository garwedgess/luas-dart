package com.wedgess.luas.domain.alarm

import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import java.time.Duration
import java.time.Instant

sealed interface AlarmMatch {
    /**
     * [ambiguous] is only ever true for Luas, where several trams on the same route can fit the same alarm. The
     * earliest candidate is used so the reminder is never later than a plausible arrival.
     */
    data class Found(val departure: Departure, val ambiguous: Boolean = false) : AlarmMatch
    data object NotFound : AlarmMatch
}

/** Base tolerance when matching a Luas tram by time: the API rounds to whole minutes and trams drift. */
private val LuasBaseWindow: Duration = Duration.ofMinutes(3)
private val LuasMaxWindow: Duration = Duration.ofMinutes(8)
private const val LUAS_DRIFT_PERCENT = 20L
private const val PERCENT = 100L

/**
 * Finds the departure an alarm refers to in a successful live result.
 *
 * - DART trains are matched by train code and operating date, never by time.
 * - The Luas API exposes no vehicle identity, so a tram is only matched by station, direction, destination and an
 *   arrival window around the last live-observed time. A service that merely shares a destination but is outside the
 *   window is not matched, and two candidates inside it are reported as ambiguous instead of picking silently.
 * - Next-departure alarms follow the earliest qualifying service and may move between services.
 */
fun matchAlarm(alarm: Alarm, live: Departures, now: Instant): AlarmMatch {
    val candidates = live.items.filter { it.transport == alarm.transport && it.matchesFilters(alarm) }
    return when (alarm.kind) {
        AlarmKind.NEXT_DEPARTURE -> matchNext(alarm, candidates, now)
        AlarmKind.SPECIFIC_DEPARTURE -> when (alarm.transport) {
            TransportType.DART -> matchDart(alarm, candidates)
            TransportType.LUAS -> matchLuas(alarm, candidates, now)
        }
    }
}

private fun Departure.matchesFilters(alarm: Alarm) =
    expectedAt != null &&
        (alarm.direction == null || direction == alarm.direction) &&
        (alarm.destination == null || destination.equals(alarm.destination, ignoreCase = true))

private fun matchNext(alarm: Alarm, candidates: List<Departure>, now: Instant): AlarmMatch {
    // Only services that can still be notified in time qualify.
    val earliestUseful = now.plus(Duration.ofMinutes(alarm.notifyMinutesBefore.toLong()))
    return candidates
        .filter { !requireNotNull(it.expectedAt).isBefore(earliestUseful) }
        .minByOrNull { requireNotNull(it.expectedAt) }
        ?.let { AlarmMatch.Found(it) }
        ?: AlarmMatch.NotFound
}

private fun matchDart(alarm: Alarm, candidates: List<Departure>): AlarmMatch {
    val tripCode = alarm.tripCode ?: return AlarmMatch.NotFound
    val found = candidates.firstOrNull {
        it.tripCode == tripCode && datesCompatible(alarm.operatingDate, it.operatingDate)
    }
    return found?.let { AlarmMatch.Found(it) } ?: AlarmMatch.NotFound
}

// A missing date on either side (an unparseable API value) cannot disprove a match; two known dates must agree.
private fun datesCompatible(a: java.time.LocalDate?, b: java.time.LocalDate?) = a == null || b == null || a == b

private fun matchLuas(alarm: Alarm, candidates: List<Departure>, now: Instant): AlarmMatch {
    val target = alarm.targetExpectedAt ?: return AlarmMatch.NotFound
    val observed = alarm.targetObservedAt ?: alarm.createdAt
    val window = luasWindow(Duration.between(observed, now))
    val inWindow = candidates.filter {
        Duration.between(target, requireNotNull(it.expectedAt)).abs() <= window
    }.sortedBy { it.expectedAt }
    return when (inWindow.size) {
        0 -> AlarmMatch.NotFound
        1 -> AlarmMatch.Found(inWindow.single())
        else -> AlarmMatch.Found(inWindow.first(), ambiguous = true)
    }
}

/** The longer ago the target was observed, the more its arrival can have drifted. */
internal fun luasWindow(sinceObserved: Duration): Duration {
    val drift = sinceObserved.coerceAtLeast(Duration.ZERO).multipliedBy(LUAS_DRIFT_PERCENT).dividedBy(PERCENT)
    return (LuasBaseWindow + drift).coerceAtMost(LuasMaxWindow)
}

private fun Duration.coerceAtLeast(min: Duration) = if (this < min) min else this
private fun Duration.coerceAtMost(max: Duration) = if (this > max) max else this
