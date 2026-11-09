package com.wedgess.luas.data.repository

import android.os.SystemClock
import com.wedgess.luas.data.alarm.AlarmManagerDataSource
import com.wedgess.luas.data.mapper.toData
import com.wedgess.luas.data.utils.extensions.resultOf
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.LuasNotificationEntity
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.AlarmConfigRepository
import com.wedgess.luas.domain.repository.AlarmRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val ALARM_SAFETY_NET = 30_000

class AlarmRepositoryImpl @Inject constructor(
    private val alarmManagerDataSource: AlarmManagerDataSource,
    private val alarmConfigRepository: AlarmConfigRepository,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
) : AlarmRepository {

    override suspend fun scheduleAlarm(
        secondsFromNow: Long,
        luasNotificationEntity: LuasNotificationEntity
    ): Result<Long> =
        withContext(dispatcher) {
            resultOf {
                // The definition is stored first: it is the source of truth the OS registration is rebuilt from.
                val alarm = luasNotificationEntity.toAlarm(clock.instant(), secondsFromNow)
                // The OS holds a single registration today, so scheduling replaces any earlier alarm.
                alarmConfigRepository.finishAllActive(AlarmState.CANCELLED).getOrThrow()
                alarmConfigRepository.save(alarm).getOrThrow()
                val triggerMillis = SystemClock.elapsedRealtime() + secondsFromNow * TimeUnit.SECONDS.toMillis(1)
                val triggerTimeMillis = triggerMillis.run {
                    this - ALARM_SAFETY_NET
                }
                // Not registered with the OS means the stored alarm must not stay active.
                resultOf {
                    alarmManagerDataSource.scheduleAlarm(
                        triggerTimeMillis = triggerTimeMillis,
                        luasNotificationData = luasNotificationEntity.toData()
                    )
                }.onFailure { alarmConfigRepository.finish(alarm.id, AlarmState.CANCELLED) }.getOrThrow()
                triggerTimeMillis
            }
        }

    override fun isAlarmRunning(): Flow<Boolean> = alarmManagerDataSource.alarmStateFlow

    override fun cancelAlarm() {
        alarmManagerDataSource.cancelAlarm()
        scope.launch { alarmConfigRepository.finishAllActive(AlarmState.CANCELLED) }
    }

    override fun alarmFired() {
        alarmManagerDataSource.cancelAlarm()
        scope.launch { alarmConfigRepository.finishAllActive(AlarmState.FIRED) }
    }

    override fun canScheduleExactAlarms(): Boolean = alarmManagerDataSource.canScheduleExactAlarms()

    override fun openExactAlarmPermissionSettings() = alarmManagerDataSource.openExactAlarmPermissionSettings()

    override fun isBatteryOptimizationIgnored(): Boolean = alarmManagerDataSource.isBatteryOptimizationIgnored()

    override fun openBatteryOptimizationSettings() = alarmManagerDataSource.openBatteryOptimizationSettings()
}

// The single OS registration identifies the alarm by where and to what it points, so scheduling it again updates it.
private fun LuasNotificationEntity.toAlarm(now: Instant, secondsFromNow: Long) = Alarm(
    id = "dep:${TransportType.LUAS.name}:$stationCode:$destination",
    kind = AlarmKind.SPECIFIC_DEPARTURE,
    state = AlarmState.ACTIVE,
    transport = TransportType.LUAS,
    stationCode = stationCode,
    stationName = station,
    destination = destination,
    notifyMinutesBefore = notifyMinutesBefore,
    targetExpectedAt = now.plusSeconds(secondsFromNow + TimeUnit.MINUTES.toSeconds(notifyMinutesBefore.toLong())),
    targetObservedAt = now,
    createdAt = now,
    updatedAt = now
)
