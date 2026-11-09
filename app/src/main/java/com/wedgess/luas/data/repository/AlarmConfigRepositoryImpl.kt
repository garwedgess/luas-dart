package com.wedgess.luas.data.repository

import com.wedgess.luas.data.db.dao.AlarmDao
import com.wedgess.luas.data.mapper.toAlarm
import com.wedgess.luas.data.mapper.toRow
import com.wedgess.luas.data.utils.extensions.resultOf
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.repository.AlarmConfigRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import javax.inject.Inject

class AlarmConfigRepositoryImpl @Inject constructor(
    private val alarmDao: AlarmDao,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AlarmConfigRepository {

    override suspend fun save(alarm: Alarm): Result<Unit> = withContext(ioDispatcher) {
        resultOf { alarmDao.save(alarm.toRow()) }
    }

    override suspend fun get(id: String): Result<Alarm?> = withContext(ioDispatcher) {
        resultOf { alarmDao.findById(id)?.toAlarm() }
    }

    override fun observeActive(): Flow<List<Alarm>> =
        alarmDao.observeByState(AlarmState.ACTIVE.name).map { rows -> rows.map { it.toAlarm() } }

    override suspend fun activeAlarms(): Result<List<Alarm>> = withContext(ioDispatcher) {
        resultOf { alarmDao.findByState(AlarmState.ACTIVE.name).map { it.toAlarm() } }
    }

    override suspend fun finish(id: String, state: AlarmState): Result<Boolean> = withContext(ioDispatcher) {
        resultOf {
            require(state != AlarmState.ACTIVE) { "An alarm is finished by moving it out of ACTIVE" }
            alarmDao.updateState(id, AlarmState.ACTIVE.name, state.name, clock.millis())
        }
    }

    override suspend fun finishAllActive(state: AlarmState): Result<Unit> = withContext(ioDispatcher) {
        resultOf {
            require(state != AlarmState.ACTIVE) { "An alarm is finished by moving it out of ACTIVE" }
            alarmDao.updateStateOfAll(AlarmState.ACTIVE.name, state.name, clock.millis())
        }
    }

    override suspend fun purgeFinished(updatedBeforeMillis: Long): Result<Unit> = withContext(ioDispatcher) {
        resultOf { alarmDao.deleteFinishedBefore(AlarmState.ACTIVE.name, updatedBeforeMillis) }
    }
}
