package com.wedgess.luas.domain.repository

import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmState
import kotlinx.coroutines.flow.Flow

/** Durable alarm definitions. Every operation is idempotent so restoration and retries cannot create duplicates. */
interface AlarmConfigRepository {

    /** Inserts or updates the alarm with this id, keeping its original creation time. */
    suspend fun save(alarm: Alarm): Result<Unit>

    suspend fun get(id: String): Result<Alarm?>

    fun observeActive(): Flow<List<Alarm>>

    suspend fun activeAlarms(): Result<List<Alarm>>

    /** Moves an ACTIVE alarm to [state]; returns false when it was not active, so repeating it is harmless. */
    suspend fun finish(id: String, state: AlarmState): Result<Boolean>

    /** Moves every ACTIVE alarm to [state]. */
    suspend fun finishAllActive(state: AlarmState): Result<Unit>

    /** Removes alarms that are no longer active and have not changed since [updatedBefore] (epoch milliseconds). */
    suspend fun purgeFinished(updatedBeforeMillis: Long): Result<Unit>
}
