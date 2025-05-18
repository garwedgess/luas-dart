package com.wedgess.luas.domain

import com.wedgess.luas.domain.repository.AlarmRepository
import com.wedgess.luas.domain.usecase.CanScheduleExactAlarmsUseCase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CanScheduleExactAlarmsUseCaseTest {

    @MockK
    private lateinit var alarmRepository: AlarmRepository
    private lateinit var canScheduleExactAlarmsUseCase: CanScheduleExactAlarmsUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        canScheduleExactAlarmsUseCase = CanScheduleExactAlarmsUseCase(alarmRepository)
    }

    @Test
    fun `invoke should return true when repository canScheduleExactAlarms returns true`() {
        // Given
        every { alarmRepository.canScheduleExactAlarms() } returns true

        // When
        val result = canScheduleExactAlarmsUseCase()

        // Then
        verify { alarmRepository.canScheduleExactAlarms() }
        assertTrue(result)
    }

    @Test
    fun `invoke should return false when repository canScheduleExactAlarms returns false`() {
        // Given
        every { alarmRepository.canScheduleExactAlarms() } returns false

        // When
        val result = canScheduleExactAlarmsUseCase()

        // Then
        verify { alarmRepository.canScheduleExactAlarms() }
        assertFalse(result)
    }

    @Test
    fun `invoke should call canScheduleExactAlarms on repository exactly once`() {
        // Given
        // We don't care about the return value for this specific verification
        every { alarmRepository.canScheduleExactAlarms() } returns true // or false, doesn't matter

        // When
        canScheduleExactAlarmsUseCase()

        // Then
        verify(exactly = 1) { alarmRepository.canScheduleExactAlarms() }
    }
}
