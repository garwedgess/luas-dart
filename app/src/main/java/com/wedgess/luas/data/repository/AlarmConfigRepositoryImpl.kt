package com.wedgess.luas.data.repository

import com.wedgess.luas.data.db.dao.AlarmDao
import com.wedgess.luas.data.mapper.toAlarm
import com.wedgess.luas.data.mapper.toRow
import com.wedgess.luas.data.utils.extensions.resultOf
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmFinishReason
import com.wedgess.luas.domain.model.AlarmRegistration
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.AlarmVerification
import com.wedgess.luas.domain.repository.AlarmConfigRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class AlarmConfigRepositoryImpl @Inject constructor(
    private val alarmDao: AlarmDao,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AlarmConfigRepository {

    private val active = AlarmState.ACTIVE.name

    override suspend fun insertIfAbsent(alarm: Alarm): Result<Alarm> = withContext(ioDispatcher) {
        resultOf { alarmDao.insertIfAbsent(alarm.toRow()).toAlarm() }
    }

    override suspend fun get(id: String): Result<Alarm?> = withContext(ioDispatcher) {
        resultOf { alarmDao.findById(id)?.toAlarm() }
    }

    override fun observeActive(): Flow<List<Alarm>> =
        alarmDao.observeByState(active).map { rows -> rows.map { it.toAlarm() } }

    override suspend fun activeAlarms(): Result<List<Alarm>> = withContext(ioDispatcher) {
        resultOf { alarmDao.findByState(active).map { it.toAlarm() } }
    }

    override suspend fun updateSchedule(
        id: String,
        reminderAt: Instant,
        targetExpectedAt: Instant?,
        targetObservedAt: Instant?,
        checkCount: Int
    ): Result<Boolean> = withContext(ioDispatcher) {
        resultOf {
            alarmDao.updateSchedule(
                id = id,
                reminderAtMillis = reminderAt.toEpochMilli(),
                targetExpectedAtMillis = targetExpectedAt?.toEpochMilli(),
                targetObservedAtMillis = targetObservedAt?.toEpochMilli(),
                checkCount = checkCount.toLong(),
                updatedAtMillis = clock.millis()
            )
        }
    }

    override suspend fun updateRegistration(id: String, registration: AlarmRegistration): Result<Boolean> =
        withContext(ioDispatcher) {
            resultOf { alarmDao.updateRegistration(id, registration.name, clock.millis()) }
        }

    override suspend fun finish(
        id: String,
        state: AlarmState,
        reason: AlarmFinishReason,
        verification: AlarmVerification?,
        notifiedAt: Instant?
    ): Result<Boolean> = withContext(ioDispatcher) {
        resultOf {
            require(state != AlarmState.ACTIVE) { "An alarm is finished by moving it out of ACTIVE" }
            alarmDao.finish(
                id = id,
                newState = state.name,
                finishReason = reason.name,
                verification = verification?.name,
                notifiedAtMillis = notifiedAt?.toEpochMilli(),
                updatedAtMillis = clock.millis()
            )
        }
    }

    override suspend fun purgeFinished(updatedBeforeMillis: Long): Result<Unit> = withContext(ioDispatcher) {
        resultOf { alarmDao.deleteFinishedBefore(updatedBeforeMillis) }
    }
}
