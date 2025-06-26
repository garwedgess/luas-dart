package com.wedgess.luas.domain

import com.wedgess.luas.domain.model.LuasNotificationEntity
import com.wedgess.luas.domain.repository.AlarmRepository
import com.wedgess.luas.domain.usecase.ScheduleAlarmUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk // For creating mock instances of data classes if needed
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail // For asserting that an exception is thrown
import org.junit.Before
import org.junit.Test

class ScheduleAlarmUseCaseTest {

    @MockK
    private lateinit var alarmRepository: AlarmRepository
    private lateinit var scheduleAlarmUseCase: ScheduleAlarmUseCase

    // Mock NotificationEntity if it has complex dependencies or behavior,
    // otherwise, creating a real instance is fine.
    private val mockLuasNotificationEntity: LuasNotificationEntity = mockk()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        scheduleAlarmUseCase = ScheduleAlarmUseCase(alarmRepository)
    }

    @Test
    fun `invoke should call scheduleAlarm on repository and return success when secondsFromNow is valid`() = runTest {
        // Given
        val secondsFromNow = 60L
        val expectedAlarmId = 12345L
        val successResult = Result.success(expectedAlarmId)
        coEvery { alarmRepository.scheduleAlarm(secondsFromNow, mockLuasNotificationEntity) } returns successResult

        // When
        val result = scheduleAlarmUseCase(secondsFromNow, mockLuasNotificationEntity)

        // Then
        coVerify { alarmRepository.scheduleAlarm(secondsFromNow, mockLuasNotificationEntity) }
        assertTrue(result.isSuccess)
        assertEquals(expectedAlarmId, result.getOrNull())
    }

    @Test
    fun `invoke should return failure result when repository scheduleAlarm fails`() = runTest {
        // Given
        val secondsFromNow = 60L
        val exception = RuntimeException("Scheduling failed")
        val failureResult = Result.failure<Long>(exception)
        coEvery { alarmRepository.scheduleAlarm(secondsFromNow, mockLuasNotificationEntity) } returns failureResult

        // When
        val result = scheduleAlarmUseCase(secondsFromNow, mockLuasNotificationEntity)

        // Then
        coVerify { alarmRepository.scheduleAlarm(secondsFromNow, mockLuasNotificationEntity) }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `invoke should throw IllegalArgumentException when secondsFromNow is 0`() = runTest {
        // Given
        val secondsFromNow = 0L

        // When & Then
        try {
            scheduleAlarmUseCase(secondsFromNow, mockLuasNotificationEntity)
            fail("Expected IllegalArgumentException was not thrown")
        } catch (e: IllegalArgumentException) {
            assertEquals("Minutes must be greater than 0", e.message)
            // Optionally, verify that the repository method was NOT called
            coVerify(exactly = 0) { alarmRepository.scheduleAlarm(any(), any()) }
        }
    }

    @Test
    fun `invoke should throw IllegalArgumentException when secondsFromNow is negative`() = runTest {
        // Given
        val secondsFromNow = -10L

        // When & Then
        try {
            scheduleAlarmUseCase(secondsFromNow, mockLuasNotificationEntity)
            fail("Expected IllegalArgumentException was not thrown")
        } catch (e: IllegalArgumentException) {
            assertEquals("Minutes must be greater than 0", e.message)
            // Optionally, verify that the repository method was NOT called
            coVerify(exactly = 0) { alarmRepository.scheduleAlarm(any(), any()) }
        }
    }

    @Test
    fun `invoke should call scheduleAlarm on repository exactly once when input is valid`() = runTest {
        // Given
        val secondsFromNow = 120L
        val expectedAlarmId = 67890L
        coEvery { alarmRepository.scheduleAlarm(secondsFromNow, mockLuasNotificationEntity) } returns
            Result.success(expectedAlarmId)

        // When
        scheduleAlarmUseCase(secondsFromNow, mockLuasNotificationEntity)

        // Then
        coVerify(exactly = 1) { alarmRepository.scheduleAlarm(secondsFromNow, mockLuasNotificationEntity) }
    }
}
