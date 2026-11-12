package com.wedgess.luas.domain.alarm

import com.wedgess.luas.domain.NetworkMonitor
import com.wedgess.luas.domain.Polled
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmVerification
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.pollFlow
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.repository.LuasRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/** A visible, delivered reminder follows its original service without creating or rescheduling any alarms. */
class LiveReminderSession @Inject constructor(
    private val luas: LuasRepository,
    private val dart: DartRepository,
    private val network: NetworkMonitor,
    private val clock: Clock
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun updates(
        alarm: Alarm,
        initial: ReminderContent.Upcoming,
        forecasts: Flow<Polled<Departures>> = forecasts(alarm)
    ): Flow<ReminderContent> = flow {
        val state = TrackingState(alarm, initial, clock.instant())
        val results = forecasts.map { Tick.Result(it.data.takeIf { _ -> it.isLive }) }
        emitAllUpdates(merge(results, ticks()), state)
    }.distinctUntilChanged()

    fun forecasts(alarm: Alarm): Flow<Polled<Departures>> =
        pollFlow(
            if (alarm.transport == TransportType.LUAS) LUAS_INTERVAL else DART_INTERVAL,
            emptyFlow(),
            online = network.isOnline
        ) {
            withTimeoutOrNull(REQUEST_TIMEOUT.toMillis()) { fetch(alarm) }
                ?: Result.failure(IOException("Live reminder request timed out"))
        }

    private suspend fun fetch(alarm: Alarm): Result<Departures> = when (alarm.transport) {
        TransportType.LUAS -> luas.fetchForecast(alarm.stationCode)
        TransportType.DART -> dart.fetchForecast(alarm.stationCode)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun kotlinx.coroutines.flow.FlowCollector<ReminderContent>.emitAllUpdates(
        events: Flow<Tick>,
        state: TrackingState
    ) {
        events.transformWhile { tick ->
            val content = state.content(tick, clock.instant())
            emit(content)
            content is ReminderContent.Upcoming
        }.collect { emit(it) }
    }
}

private sealed interface Tick {
    data object Clock : Tick
    data class Result(val departures: Departures?) : Tick
}

private fun ticks(): Flow<Tick> = flow {
    while (true) {
        emit(Tick.Clock)
        delay(CLOCK_TICK_MILLIS)
    }
}

private class TrackingState(alarm: Alarm, initial: ReminderContent.Upcoming, startedAt: Instant) {
    private var target = alarm.copy(
        kind = AlarmKind.SPECIFIC_DEPARTURE,
        targetExpectedAt = initial.expectedAt ?: alarm.targetExpectedAt
    )
    private var current = initial.copy(
        lastCheckedAt = initial.lastCheckedAt ?: startedAt.takeIf {
            initial.verification == AlarmVerification.LIVE_VERIFIED
        }
    )
    private val deadline = startedAt.plus(MAX_TRACKING_DURATION)

    fun content(tick: Tick, now: Instant): ReminderContent {
        if (tick is Tick.Result) {
            val terminal = update(tick.departures, now)
            if (terminal != null) return terminal
        }
        if (expired(now)) return ReminderContent.TrackingFinished(current.expectedAt)
        if (current.lastCheckedAt?.plus(MAX_LIVE_AGE)?.isBefore(now) == true) {
            current = current.copy(verification = AlarmVerification.UNVERIFIED)
        }
        current = current.copy(countdownFinished = current.expectedAt?.isAfter(now) == false)
        return current
    }

    private fun update(departures: Departures?, now: Instant): ReminderContent? {
        if (departures == null || !departures.isFor(target)) {
            current = current.copy(verification = AlarmVerification.UNVERIFIED)
            return null
        }
        val found = matchAlarm(target, departures, now) as? AlarmMatch.Found
        if (found == null) {
            return if (expired(now)) {
                ReminderContent.TrackingFinished(current.expectedAt)
            } else {
                ReminderContent.ServiceLost
            }
        }
        val departure = found.departure
        current = ReminderContent.Upcoming(
            departure.expectedAt,
            AlarmVerification.LIVE_VERIFIED,
            found.ambiguous,
            now
        )
        // Pin a NEXT_DEPARTURE reminder once delivered. Never follow the next train after this one disappears.
        if (!found.ambiguous) {
            target = target.copy(
                kind = AlarmKind.SPECIFIC_DEPARTURE,
                direction = departure.direction,
                destination = departure.destination,
                tripCode = departure.tripCode,
                operatingDate = departure.operatingDate,
                targetExpectedAt = departure.expectedAt,
                targetObservedAt = now
            )
        }
        return null
    }

    private fun expired(now: Instant): Boolean = !now.isBefore(deadline) ||
        current.expectedAt?.plus(MissedGrace)?.let { !now.isBefore(it) } == true
}

private fun Departures.isFor(alarm: Alarm): Boolean =
    if (stationCode.isBlank()) items.isEmpty() else stationCode.trim().equals(alarm.stationCode.trim(), true)

private const val LUAS_INTERVAL = 15_000L
private const val DART_INTERVAL = 40_000L
private const val CLOCK_TICK_MILLIS = 1_000L
private val MAX_TRACKING_DURATION: Duration = Duration.ofMinutes(30)
private val MAX_LIVE_AGE: Duration = Duration.ofSeconds(70)

// Continuous foreground tracking is not bound by the receiver's six-second delivery deadline.
private val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(20)
