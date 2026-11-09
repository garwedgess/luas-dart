package com.wedgess.luas.data.db.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.wedgess.luas.Alarm
import com.wedgess.luas.data.LuasDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class AlarmDao @Inject constructor(
    private val db: LuasDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val queries = db.alarmQueries

    fun findById(id: String): Alarm? = queries.selectById(id).executeAsOneOrNull()

    fun findByState(state: String): List<Alarm> = queries.selectByState(state).executeAsList()

    fun observeByState(state: String) = queries.selectByState(state).asFlow().mapToList(ioDispatcher)

    /**
     * Inserts the alarm with the next free request code, unless the id already exists, and returns the stored row.
     * Runs in one transaction, so concurrent callers get distinct codes and a repeated id never overwrites.
     */
    fun insertIfAbsent(alarm: Alarm): Alarm = db.transactionWithResult {
        val code = queries.nextRequestCode().executeAsOne()
        queries.insertIfAbsent(
            Id = alarm.Id,
            RequestCode = code,
            Kind = alarm.Kind,
            State = alarm.State,
            Transport = alarm.Transport,
            StationCode = alarm.StationCode,
            StationName = alarm.StationName,
            Direction = alarm.Direction,
            Destination = alarm.Destination,
            NotifyMinutesBefore = alarm.NotifyMinutesBefore,
            TripCode = alarm.TripCode,
            OperatingDate = alarm.OperatingDate,
            TargetExpectedAtMillis = alarm.TargetExpectedAtMillis,
            TargetObservedAtMillis = alarm.TargetObservedAtMillis,
            ReminderAtMillis = alarm.ReminderAtMillis,
            CheckCount = alarm.CheckCount,
            Registration = alarm.Registration,
            FinishReason = alarm.FinishReason,
            Verification = alarm.Verification,
            NotifiedAtMillis = alarm.NotifiedAtMillis,
            CreatedAtMillis = alarm.CreatedAtMillis,
            UpdatedAtMillis = alarm.UpdatedAtMillis
        )
        queries.selectById(alarm.Id).executeAsOne()
    }

    // The parameters map one-to-one onto the columns being written.
    @Suppress("LongParameterList")
    fun updateSchedule(
        id: String,
        reminderAtMillis: Long,
        targetExpectedAtMillis: Long?,
        targetObservedAtMillis: Long?,
        checkCount: Long,
        updatedAtMillis: Long
    ): Boolean = db.transactionWithResult {
        queries.updateSchedule(
            reminderAtMillis,
            targetExpectedAtMillis,
            targetObservedAtMillis,
            checkCount,
            updatedAtMillis,
            id,
            ACTIVE
        )
        queries.changes().executeAsOne() > 0
    }

    fun updateRegistration(id: String, registration: String, updatedAtMillis: Long): Boolean =
        db.transactionWithResult {
            queries.updateRegistration(registration, updatedAtMillis, id, ACTIVE)
            queries.changes().executeAsOne() > 0
        }

    /** Moves an ACTIVE alarm to a finished state. Returns true for exactly one of several racing callers. */
    @Suppress("LongParameterList")
    fun finish(
        id: String,
        newState: String,
        finishReason: String,
        verification: String?,
        notifiedAtMillis: Long?,
        updatedAtMillis: Long
    ): Boolean = db.transactionWithResult {
        queries.finish(newState, finishReason, verification, notifiedAtMillis, updatedAtMillis, id, ACTIVE)
        queries.changes().executeAsOne() > 0
    }

    fun deleteFinishedBefore(updatedBeforeMillis: Long) = queries.deleteFinishedBefore(ACTIVE, updatedBeforeMillis)

    private companion object {
        const val ACTIVE = "ACTIVE"
    }
}
