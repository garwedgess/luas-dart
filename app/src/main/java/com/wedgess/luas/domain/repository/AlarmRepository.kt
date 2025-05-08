package com.wedgess.luas.domain.repository

import com.wedgess.luas.domain.model.NotificationEntity
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    suspend fun scheduleAlarm(secondsFromNow: Long, notificationEntity: NotificationEntity): Result<Long>
    fun isAlarmRunning(): Flow<Boolean>
    fun cancelAlarm()
    fun canScheduleExactAlarms(): Boolean
    fun isBatteryOptimizationIgnored(): Boolean
    fun openBatteryOptimizationSettings()
    fun openExactAlarmPermissionSettings()
}
