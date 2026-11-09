package com.wedgess.luas.domain.alarm

import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmFinishReason
import com.wedgess.luas.domain.model.AlarmRegistration
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.AlarmVerification
import com.wedgess.luas.domain.repository.AlarmConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class MutableClock(var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = now
}

/** In-memory repository with the same atomic, conditional transitions as the SQL implementation. */
class FakeAlarmConfigRepository(private val clock: Clock) : AlarmConfigRepository {

    private val rows = MutableStateFlow<Map<String, Alarm>>(emptyMap())
    val all: List<Alarm> get() = rows.value.values.toList()

    // No suspension happens between reading and writing a row, so each operation is atomic like a transaction.
    private fun mutate(id: String, change: (Alarm) -> Alarm): Boolean {
        val current = rows.value[id]?.takeIf { it.state == AlarmState.ACTIVE } ?: return false
        rows.value = rows.value + (id to change(current).copy(updatedAt = clock.instant()))
        return true
    }

    override suspend fun insertIfAbsent(alarm: Alarm): Result<Alarm> {
        rows.value[alarm.id]?.let { return Result.success(it) }
        val code = (rows.value.values.maxOfOrNull { it.requestCode } ?: 0) + 1
        val stored = alarm.copy(requestCode = code)
        rows.value = rows.value + (alarm.id to stored)
        return Result.success(stored)
    }

    override suspend fun get(id: String): Result<Alarm?> = Result.success(rows.value[id])

    override fun observeActive(): Flow<List<Alarm>> =
        rows.map { map -> map.values.filter { it.state == AlarmState.ACTIVE } }

    override suspend fun activeAlarms(): Result<List<Alarm>> =
        Result.success(rows.value.values.filter { it.state == AlarmState.ACTIVE }.sortedBy { it.reminderAt })

    override suspend fun updateSchedule(
        id: String,
        reminderAt: Instant,
        targetExpectedAt: Instant?,
        targetObservedAt: Instant?,
        checkCount: Int
    ): Result<Boolean> = Result.success(
        mutate(id) {
            it.copy(
                reminderAt = reminderAt,
                targetExpectedAt = targetExpectedAt,
                targetObservedAt = targetObservedAt,
                checkCount = checkCount
            )
        }
    )

    override suspend fun updateRegistration(id: String, registration: AlarmRegistration): Result<Boolean> =
        Result.success(mutate(id) { it.copy(registration = registration) })

    override suspend fun finish(
        id: String,
        state: AlarmState,
        reason: AlarmFinishReason,
        verification: AlarmVerification?,
        notifiedAt: Instant?
    ): Result<Boolean> {
        require(state != AlarmState.ACTIVE)
        return Result.success(
            mutate(id) {
                it.copy(state = state, finishReason = reason, verification = verification, notifiedAt = notifiedAt)
            }
        )
    }

    override suspend fun purgeFinished(updatedBeforeMillis: Long): Result<Unit> {
        rows.value = rows.value.filterValues {
            it.state == AlarmState.ACTIVE || it.updatedAt.toEpochMilli() >= updatedBeforeMillis
        }
        return Result.success(Unit)
    }
}

class FakeAlarmScheduler : AlarmScheduler {
    var exactAllowed = true
    var failWith: Throwable? = null
    val registrations = mutableMapOf<String, Pair<Instant, AlarmRegistration>>()
    var registerCalls = 0
    val cancelled = mutableListOf<String>()

    override fun canScheduleExactAlarms() = exactAllowed

    override fun register(alarm: Alarm, triggerAt: Instant): AlarmRegistration {
        registerCalls++
        failWith?.let { throw it }
        val registration = if (exactAllowed) AlarmRegistration.EXACT else AlarmRegistration.INEXACT
        registrations[alarm.id] = triggerAt to registration
        return registration
    }

    override fun cancel(alarmId: String) {
        registrations.remove(alarmId)
        cancelled += alarmId
    }

    override fun openExactAlarmSettings() = Unit
}

class FakeAlarmNotifier : AlarmNotifier {
    var enabled = true
    val posted = mutableListOf<Pair<Alarm, ReminderContent>>()
    val dismissed = mutableListOf<Int>()

    override fun areNotificationsEnabled() = enabled

    override fun notify(alarm: Alarm, content: ReminderContent): Boolean {
        if (!enabled) return false
        posted += alarm to content
        return true
    }

    override fun dismiss(alarm: Alarm) {
        dismissed += alarm.requestCode
    }
}
