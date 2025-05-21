package com.wedgess.luas.data.repository

import android.os.SystemClock
import com.wedgess.luas.data.alarm.AlarmManagerDataSource
import com.wedgess.luas.data.mapper.toData
import com.wedgess.luas.domain.model.NotificationEntity
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AlarmRepositoryImplTest {

    private lateinit var alarmManagerDataSource: AlarmManagerDataSource
    private lateinit var alarmRepository: AlarmRepositoryImpl
    private val testDispatcher = UnconfinedTestDispatcher()
    private val alarmStateFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        alarmManagerDataSource = mockk()
        every { alarmManagerDataSource.alarmStateFlow } returns alarmStateFlow

        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 1000L

        alarmRepository = AlarmRepositoryImpl(
            alarmManagerDataSource = alarmManagerDataSource,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `scheduleAlarm should calculate correct trigger time and call data source`() = runTest {
        // Given
        val secondsFromNow = 60L
        val notificationEntity = NotificationEntity(
            dueInMins = 10,
            station = "STSG",
            destination = "CKM",
            notifyMinutesBefore = 9
        )
        val notificationData = notificationEntity.toData()
        val expectedTriggerTime = 60_000L + 1000L - 30_000L // (secondsFromNow * 1000) + elapsedRealtime - SAFETY_NET

        every { alarmManagerDataSource.scheduleAlarm(any(), any()) } just Runs

        // When
        val result = alarmRepository.scheduleAlarm(secondsFromNow, notificationEntity)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTriggerTime, result.getOrNull())
        verify { alarmManagerDataSource.scheduleAlarm(expectedTriggerTime, notificationData) }
    }

    @Test
    fun `scheduleAlarm should return failure result when exception occurs`() = runTest {
        // Given
        val secondsFromNow = 60L
        val notificationEntity = NotificationEntity(
            dueInMins = 10,
            station = "STSG",
            destination = "CKM",
            notifyMinutesBefore = 5
        )
        val expectedException = RuntimeException("Test exception")

        every { alarmManagerDataSource.scheduleAlarm(any(), any()) } throws expectedException

        // When
        val result = alarmRepository.scheduleAlarm(secondsFromNow, notificationEntity)

        // Then
        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
    }

    @Test
    fun `isAlarmRunning should return flow from data source`() = runTest {
        // Given
        alarmStateFlow.value = true

        // When
        val result = alarmRepository.isAlarmRunning().first()

        // Then
        assertTrue(result)
    }

    @Test
    fun `cancelAlarm should call data source method`() {
        // Given
        every { alarmManagerDataSource.cancelAlarm() } just Runs

        // When
        alarmRepository.cancelAlarm()

        // Then
        verify(exactly = 1) { alarmManagerDataSource.cancelAlarm() }
    }

    @Test
    fun `canScheduleExactAlarms should return result from data source`() {
        // Given
        every { alarmManagerDataSource.canScheduleExactAlarms() } returns true

        // When
        val result = alarmRepository.canScheduleExactAlarms()

        // Then
        assertTrue(result)
        verify { alarmManagerDataSource.canScheduleExactAlarms() }
    }

    @Test
    fun `openExactAlarmPermissionSettings should call data source method`() {
        // Given
        every { alarmManagerDataSource.openExactAlarmPermissionSettings() } just Runs

        // When
        alarmRepository.openExactAlarmPermissionSettings()

        // Then
        verify(exactly = 1) { alarmManagerDataSource.openExactAlarmPermissionSettings() }
    }

    @Test
    fun `isBatteryOptimizationIgnored should return result from data source`() {
        // Given
        every { alarmManagerDataSource.isBatteryOptimizationIgnored() } returns false

        // When
        val result = alarmRepository.isBatteryOptimizationIgnored()

        // Then
        assertFalse(result)
        verify { alarmManagerDataSource.isBatteryOptimizationIgnored() }
    }

    @Test
    fun `openBatteryOptimizationSettings should call data source method`() {
        // Given
        every { alarmManagerDataSource.openBatteryOptimizationSettings() } just Runs

        // When
        alarmRepository.openBatteryOptimizationSettings()

        // Then
        verify(exactly = 1) { alarmManagerDataSource.openBatteryOptimizationSettings() }
    }
}
