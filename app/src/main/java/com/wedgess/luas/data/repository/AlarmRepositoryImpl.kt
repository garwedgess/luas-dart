package com.wedgess.luas.data.repository

import android.os.SystemClock
import com.wedgess.luas.data.alarm.AlarmManagerDataSource
import com.wedgess.luas.data.mapper.toData
import com.wedgess.luas.data.utils.extensions.resultOf
import com.wedgess.luas.domain.model.LuasNotificationEntity
import com.wedgess.luas.domain.repository.AlarmRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val ALARM_SAFETY_NET = 30_000

class AlarmRepositoryImpl @Inject constructor(
    private val alarmManagerDataSource: AlarmManagerDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : AlarmRepository {

    override suspend fun scheduleAlarm(secondsFromNow: Long, luasNotificationEntity: LuasNotificationEntity): Result<Long> =
        withContext(dispatcher) {
            resultOf {
                val triggerMillis = SystemClock.elapsedRealtime() + secondsFromNow * TimeUnit.SECONDS.toMillis(1)
                val triggerTimeMillis = triggerMillis.run {
                    this - ALARM_SAFETY_NET
                }
                alarmManagerDataSource.scheduleAlarm(
                    triggerTimeMillis = triggerTimeMillis,
                    luasNotificationData = luasNotificationEntity.toData()
                )
                triggerTimeMillis
            }
        }

    override fun isAlarmRunning(): Flow<Boolean> = alarmManagerDataSource.alarmStateFlow

    override fun cancelAlarm() = alarmManagerDataSource.cancelAlarm()

    override fun canScheduleExactAlarms(): Boolean = alarmManagerDataSource.canScheduleExactAlarms()

    override fun openExactAlarmPermissionSettings() = alarmManagerDataSource.openExactAlarmPermissionSettings()

    override fun isBatteryOptimizationIgnored(): Boolean = alarmManagerDataSource.isBatteryOptimizationIgnored()

    override fun openBatteryOptimizationSettings() = alarmManagerDataSource.openBatteryOptimizationSettings()
}
