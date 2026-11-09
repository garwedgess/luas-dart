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

    /** Inserts or replaces by id, keeping the creation time of an existing row. */
    fun save(alarm: Alarm) = db.transaction {
        val createdAt = queries.selectById(alarm.Id).executeAsOneOrNull()?.CreatedAtMillis ?: alarm.CreatedAtMillis
        queries.upsert(
            Id = alarm.Id,
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
            CreatedAtMillis = createdAt,
            UpdatedAtMillis = alarm.UpdatedAtMillis
        )
    }

    /** Returns true if a row moved from [fromState] to [newState]. */
    fun updateState(id: String, fromState: String, newState: String, updatedAtMillis: Long): Boolean =
        db.transactionWithResult {
            queries.updateState(newState, updatedAtMillis, id, fromState)
            queries.changes().executeAsOne() > 0
        }

    fun updateStateOfAll(fromState: String, newState: String, updatedAtMillis: Long) =
        queries.updateStateOfAll(newState, updatedAtMillis, fromState)

    fun deleteFinishedBefore(activeState: String, updatedBeforeMillis: Long) =
        queries.deleteFinishedBefore(activeState, updatedBeforeMillis)
}
