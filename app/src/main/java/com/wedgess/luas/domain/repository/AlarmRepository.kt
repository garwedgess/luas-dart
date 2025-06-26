package com.wedgess.luas.domain.repository

import com.wedgess.luas.domain.model.LuasNotificationEntity
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    suspend fun scheduleAlarm(secondsFromNow: Long, luasNotificationEntity: LuasNotificationEntity): Result<Long>
    fun isAlarmRunning(): Flow<Boolean>
    fun cancelAlarm()
    fun canScheduleExactAlarms(): Boolean
    fun isBatteryOptimizationIgnored(): Boolean
    fun openBatteryOptimizationSettings()
    fun openExactAlarmPermissionSettings()
}
