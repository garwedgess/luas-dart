package com.wedgess.luas.domain.repository

import com.wedgess.luas.domain.model.LuasNotificationEntity
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    suspend fun scheduleAlarm(secondsFromNow: Long, luasNotificationEntity: LuasNotificationEntity): Result<Long>
    fun isAlarmRunning(): Flow<Boolean>
    fun cancelAlarm()

    /** The OS alarm has fired: clears its registration and records the stored alarm as FIRED, not cancelled. */
    fun alarmFired()

    fun canScheduleExactAlarms(): Boolean
    fun isBatteryOptimizationIgnored(): Boolean
    fun openBatteryOptimizationSettings()
    fun openExactAlarmPermissionSettings()
}
