package com.wedgess.luas.domain.alarm

import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmCapabilities
import com.wedgess.luas.domain.model.AlarmFinishReason
import com.wedgess.luas.domain.model.AlarmRequest
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.AlarmConfigRepository
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.repository.LuasRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the alarm lifecycle. The database is authoritative: every transition is stored first, and OS registrations
 * are derived from it, so they can always be rebuilt ([reconcile]) after a reboot, update or process death.
 *
 * At most one OS wake-up is pending per alarm. A bounded number of live re-checks (see [Alarm.MAX_LIVE_CHECKS])
 * follow delays, then the delivery wake-up fires the reminder. A failed re-check is never retried on its own; the
 * delivery still happens, marked as not live-verified. Nothing here depends on a ViewModel or running UI.
 */
@Singleton
class AlarmCoordinator @Inject constructor(
    private val alarms: AlarmConfigRepository,
    private val scheduler: AlarmScheduler,
    private val notifier: AlarmNotifier,
    private val luas: LuasRepository,
    private val dart: DartRepository,
    private val clock: Clock
) {

    /** What the device currently allows; read when needed, never cached. */
    val capabilities: AlarmCapabilities
        get() = AlarmCapabilities(scheduler.canScheduleExactAlarms(), notifier.areNotificationsEnabled())

    fun observeActive(): Flow<List<Alarm>> = alarms.observeActive()

    fun openExactAlarmSettings() = scheduler.openExactAlarmSettings()

    /** Stores the alarm and registers it. Repeating the same [AlarmRequest.requestId] changes nothing. */
    suspend fun create(request: AlarmRequest): Result<Alarm> = attempt {
        require(request.notifyMinutesBefore > 0) { "Notify minutes must be greater than 0" }
        val now = clock.instant()
        val stored = alarms.insertIfAbsent(request.toAlarm(now)).getOrThrow()
        if (stored.state == AlarmState.ACTIVE) register(stored, now)
        stored
    }

    /** Cancels an alarm. Safe to repeat and safe against a reminder that is firing at the same moment. */
    suspend fun cancel(id: String): Result<Boolean> = attempt {
        val alarm = alarms.get(id).getOrThrow()
        val cancelled = alarms.finish(id, AlarmState.CANCELLED, AlarmFinishReason.USER_CANCELLED).getOrThrow()
        // Always clear the OS side as well, so a registration from a lost race cannot linger.
        scheduler.cancel(id)
        alarm?.let(notifier::dismiss)
        cancelled
    }

    /** Removes the shown notification only; the alarm itself has already finished. */
    suspend fun dismissNotification(id: String) {
        alarms.get(id).getOrNull()?.let(notifier::dismiss)
    }

    /** The OS woke us for this alarm. Does nothing unless it is still ACTIVE, so duplicate wake-ups are harmless. */
    suspend fun onAlarmDue(id: String) {
        val alarm = alarms.get(id).getOrNull()?.takeIf { it.state == AlarmState.ACTIVE } ?: return
        val now = clock.instant()
        val live = withTimeoutOrNull(LiveTimeout.toMillis()) {
            when (alarm.transport) {
                TransportType.LUAS -> luas.fetchForecast(alarm.stationCode)
                TransportType.DART -> dart.fetchForecast(alarm.stationCode)
            }.getOrNull()
        }
        val match = live?.let { matchAlarm(alarm, it, now) }
        if (now.isBefore(alarm.reminderAt.minus(FireSlack))) {
            recheck(alarm, match, now)
        } else {
            deliver(alarm, live != null, match, now)
        }
    }

    /**
     * Rebuilds the OS registrations from the database after a reboot, update, clock or permission change or process
     * start. ACTIVE alarms are re-registered (replacing any existing registration) or, if already overdue, delivered.
     * Cancelled and finished alarms are never touched, so repeating this is idempotent.
     */
    suspend fun reconcile() {
        val now = clock.instant()
        alarms.activeAlarms().getOrNull().orEmpty().forEach { alarm ->
            attempt {
                if (now.isBefore(alarm.reminderAt)) register(alarm, now) else onAlarmDue(alarm.id)
            }.onFailure { Timber.e(it, "Failed to restore alarm ${alarm.id}") }
        }
        alarms.purgeFinished(now.minus(FinishedRetention).toEpochMilli())
    }

    /** An early wake-up: follow the live result (if any), count the attempt, and wait for the delivery. */
    private suspend fun recheck(alarm: Alarm, match: AlarmMatch?, now: Instant) {
        val found = (match as? AlarmMatch.Found)?.departure
        val expected = found?.expectedAt ?: alarm.targetExpectedAt
        val reminder = expected?.takeIf { found != null }?.let { reminderTime(it, alarm.notifyMinutesBefore) }
        val updated = alarms.updateSchedule(
            id = alarm.id,
            reminderAt = reminder ?: alarm.reminderAt,
            targetExpectedAt = expected,
            targetObservedAt = if (found != null) now else alarm.targetObservedAt,
            checkCount = alarm.checkCount + 1
        ).getOrThrow()
        if (!updated) return // cancelled or finished in the meantime
        val current = alarms.get(alarm.id).getOrThrow() ?: return
        if (!now.isBefore(current.reminderAt.minus(FireSlack))) {
            deliver(current, hadLive = match != null, match = match, now = now)
        } else {
            register(current, now)
        }
    }

    private suspend fun deliver(alarm: Alarm, hadLive: Boolean, match: AlarmMatch?, now: Instant) {
        when (val decision = decideDelivery(alarm, hadLive, match, now)) {
            is DeliveryDecision.Defer -> {
                val expected = requireNotNull(decision.found.departure.expectedAt)
                val updated = alarms.updateSchedule(
                    alarm.id,
                    reminderTime(expected, alarm.notifyMinutesBefore),
                    expected,
                    now,
                    alarm.checkCount + 1
                ).getOrThrow()
                if (updated) alarms.get(alarm.id).getOrThrow()?.let { register(it, now) }
            }

            is DeliveryDecision.Notify -> {
                val deliveredAt = clock.instant()
                if (!notifier.areNotificationsEnabled()) {
                    // Notification permission is separate from exact alarms; without it nothing can be shown.
                    alarms.finish(
                        alarm.id,
                        AlarmState.FIRED,
                        AlarmFinishReason.NOTIFICATIONS_DISABLED,
                        decision.verification,
                        deliveredAt
                    )
                } else if (
                    alarms.finish(alarm.id, decision.state, decision.reason, decision.verification, deliveredAt)
                        .getOrThrow()
                ) {
                    // Only the caller that won the transition shows the notification, so repeats cannot duplicate it.
                    notifier.notify(alarm, decision.content)
                }
                scheduler.cancel(alarm.id)
            }
        }
    }

    private suspend fun register(alarm: Alarm, now: Instant) {
        val registration = attempt { scheduler.register(alarm, nextTrigger(alarm, now)) }.getOrElse {
            alarms.finish(alarm.id, AlarmState.EXPIRED, AlarmFinishReason.REGISTRATION_FAILED)
            throw it
        }
        alarms.updateRegistration(alarm.id, registration)
        // Cancelled while registering: drop the registration that was just made.
        if (alarms.get(alarm.id).getOrNull()?.state != AlarmState.ACTIVE) scheduler.cancel(alarm.id)
    }
}

private fun AlarmRequest.toAlarm(now: Instant) = Alarm(
    id = requestId,
    requestCode = 0, // assigned when stored
    kind = kind,
    state = AlarmState.ACTIVE,
    transport = transport,
    stationCode = stationCode,
    stationName = stationName,
    direction = direction,
    destination = destination,
    notifyMinutesBefore = notifyMinutesBefore,
    tripCode = tripCode,
    operatingDate = operatingDate,
    targetExpectedAt = expectedAt,
    targetObservedAt = observedAt,
    reminderAt = reminderTime(expectedAt, notifyMinutesBefore),
    createdAt = now,
    updatedAt = now
)

@Suppress("TooGenericExceptionCaught")
private inline fun <T> attempt(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
