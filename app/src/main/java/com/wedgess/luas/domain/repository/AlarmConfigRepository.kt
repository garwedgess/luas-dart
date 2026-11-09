package com.wedgess.luas.domain.repository

import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmFinishReason
import com.wedgess.luas.domain.model.AlarmRegistration
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.AlarmVerification
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Durable alarm definitions. Every state change is a single atomic conditional update that only applies to an
 * ACTIVE alarm, so a cancelled or finished alarm can never be resurrected or notified twice, and exactly one of two
 * racing callers wins a transition.
 */
interface AlarmConfigRepository {

    /** Stores the alarm, assigning its [Alarm.requestCode]; returns the already stored alarm if the id exists. */
    suspend fun insertIfAbsent(alarm: Alarm): Result<Alarm>

    suspend fun get(id: String): Result<Alarm?>

    fun observeActive(): Flow<List<Alarm>>

    suspend fun activeAlarms(): Result<List<Alarm>>

    /** Updates the reminder time and live-observed target of an ACTIVE alarm; false if it is not active. */
    suspend fun updateSchedule(
        id: String,
        reminderAt: Instant,
        targetExpectedAt: Instant?,
        targetObservedAt: Instant?,
        checkCount: Int
    ): Result<Boolean>

    suspend fun updateRegistration(id: String, registration: AlarmRegistration): Result<Boolean>

    /** Moves an ACTIVE alarm to a finished [state]; returns false when it was not active (the caller lost). */
    suspend fun finish(
        id: String,
        state: AlarmState,
        reason: AlarmFinishReason,
        verification: AlarmVerification? = null,
        notifiedAt: Instant? = null
    ): Result<Boolean>

    /** Removes alarms that are no longer active and have not changed since [updatedBeforeMillis]. */
    suspend fun purgeFinished(updatedBeforeMillis: Long): Result<Unit>
}
