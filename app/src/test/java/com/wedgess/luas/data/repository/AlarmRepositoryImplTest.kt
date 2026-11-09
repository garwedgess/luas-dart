package com.wedgess.luas.data.repository

import android.os.SystemClock
import com.wedgess.luas.data.alarm.AlarmManagerDataSource
import com.wedgess.luas.data.mapper.toData
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.LuasNotificationEntity
import com.wedgess.luas.domain.repository.AlarmConfigRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@ExperimentalCoroutinesApi
class AlarmRepositoryImplTest {

    private lateinit var alarmManagerDataSource: AlarmManagerDataSource
    private lateinit var alarmRepository: AlarmRepositoryImpl
    private lateinit var alarmConfigRepository: AlarmConfigRepository
    private val now = Instant.parse("2026-11-09T22:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val alarmStateFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        alarmManagerDataSource = mockk()
        every { alarmManagerDataSource.alarmStateFlow } returns alarmStateFlow

        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 1000L

        alarmConfigRepository = mockk(relaxed = true)
        coEvery { alarmConfigRepository.finishAllActive(any()) } returns Result.success(Unit)
        coEvery { alarmConfigRepository.save(any()) } returns Result.success(Unit)
        coEvery { alarmConfigRepository.finish(any(), any()) } returns Result.success(true)

        alarmRepository = AlarmRepositoryImpl(
            alarmManagerDataSource = alarmManagerDataSource,
            alarmConfigRepository = alarmConfigRepository,
            clock = clock,
            dispatcher = testDispatcher,
            scope = CoroutineScope(testDispatcher)
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
        val luasNotificationEntity = LuasNotificationEntity(
            dueInMins = 10,
            station = "STSG",
            destination = "CKM",
            notifyMinutesBefore = 9
        )
        val notificationData = luasNotificationEntity.toData()
        val expectedTriggerTime = 60_000L + 1000L - 30_000L // (secondsFromNow * 1000) + elapsedRealtime - SAFETY_NET

        every { alarmManagerDataSource.scheduleAlarm(any(), any()) } just Runs

        // When
        val result = alarmRepository.scheduleAlarm(secondsFromNow, luasNotificationEntity)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTriggerTime, result.getOrNull())
        verify { alarmManagerDataSource.scheduleAlarm(expectedTriggerTime, notificationData) }
    }

    @Test
    fun `scheduleAlarm should return failure result when exception occurs`() = runTest {
        // Given
        val secondsFromNow = 60L
        val luasNotificationEntity = LuasNotificationEntity(
            dueInMins = 10,
            station = "STSG",
            destination = "CKM",
            notifyMinutesBefore = 5
        )
        val expectedException = RuntimeException("Test exception")

        every { alarmManagerDataSource.scheduleAlarm(any(), any()) } throws expectedException

        // When
        val result = alarmRepository.scheduleAlarm(secondsFromNow, luasNotificationEntity)

        // Then
        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
    }

    @Test
    fun `scheduleAlarm stores an active alarm before registering with the OS and supersedes earlier ones`() = runTest {
        every { alarmManagerDataSource.scheduleAlarm(any(), any()) } just Runs
        val saved = slot<Alarm>()
        val entity = LuasNotificationEntity(
            dueInMins = 10,
            station = "Stillorgan",
            stationCode = "STI",
            destination = "Broombridge",
            notifyMinutesBefore = 4
        )

        alarmRepository.scheduleAlarm(secondsFromNow = 360, luasNotificationEntity = entity)

        coVerifyOrder {
            alarmConfigRepository.finishAllActive(AlarmState.CANCELLED)
            alarmConfigRepository.save(capture(saved))
            alarmManagerDataSource.scheduleAlarm(any(), any())
        }
        with(saved.captured) {
            assertEquals("dep:LUAS:STI:Broombridge", id)
            assertEquals(AlarmKind.SPECIFIC_DEPARTURE, kind)
            assertEquals(AlarmState.ACTIVE, state)
            assertEquals("STI", stationCode)
            assertEquals(4, notifyMinutesBefore)
            // Arrival = now + time to the reminder + the lead time.
            assertEquals(now.plusSeconds(360 + 4 * 60L), targetExpectedAt)
            assertEquals(now, targetObservedAt)
        }
    }

    @Test
    fun `scheduling the same alarm twice stores the same id`() = runTest {
        every { alarmManagerDataSource.scheduleAlarm(any(), any()) } just Runs
        val saved = mutableListOf<Alarm>()
        coEvery { alarmConfigRepository.save(capture(saved)) } returns Result.success(Unit)
        val entity = LuasNotificationEntity(10, "Stillorgan", "STI", "Broombridge", 4)

        alarmRepository.scheduleAlarm(120, entity)
        alarmRepository.scheduleAlarm(120, entity)

        assertEquals(1, saved.map { it.id }.toSet().size)
    }

    @Test
    fun `a failed OS registration does not leave an active stored alarm`() = runTest {
        every { alarmManagerDataSource.scheduleAlarm(any(), any()) } throws SecurityException("no exact alarms")
        val entity = LuasNotificationEntity(10, "Stillorgan", "STI", "Broombridge", 4)

        val result = alarmRepository.scheduleAlarm(120, entity)

        assertTrue(result.isFailure)
        coVerify { alarmConfigRepository.finish("dep:LUAS:STI:Broombridge", AlarmState.CANCELLED) }
    }

    @Test
    fun `cancelAlarm records the stored alarm as cancelled`() = runTest {
        every { alarmManagerDataSource.cancelAlarm() } just Runs

        alarmRepository.cancelAlarm()

        coVerify { alarmConfigRepository.finishAllActive(AlarmState.CANCELLED) }
    }

    @Test
    fun `alarmFired clears the registration and records the stored alarm as fired`() = runTest {
        every { alarmManagerDataSource.cancelAlarm() } just Runs

        alarmRepository.alarmFired()

        verify(exactly = 1) { alarmManagerDataSource.cancelAlarm() }
        coVerify { alarmConfigRepository.finishAllActive(AlarmState.FIRED) }
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
