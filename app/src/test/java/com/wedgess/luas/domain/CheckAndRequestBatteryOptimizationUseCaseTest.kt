package com.wedgess.luas.domain

import com.wedgess.luas.domain.repository.AlarmRepository
import com.wedgess.luas.domain.usecase.CheckAndRequestBatteryOptimizationUseCase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun // For mocking void functions
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class CheckAndRequestBatteryOptimizationUseCaseTest {

    @MockK
    private lateinit var alarmRepository: AlarmRepository // Renamed for consistency with the use case
    private lateinit var checkAndRequestBatteryOptimizationUseCase: CheckAndRequestBatteryOptimizationUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        checkAndRequestBatteryOptimizationUseCase = CheckAndRequestBatteryOptimizationUseCase(alarmRepository)
    }

    @Test
    fun `invoke should open battery optimization settings when battery optimization is NOT ignored`() {
        // Given
        every { alarmRepository.isBatteryOptimizationIgnored() } returns false
        // Use justRun for void functions that don't return anything
        justRun { alarmRepository.openBatteryOptimizationSettings() }

        // When
        checkAndRequestBatteryOptimizationUseCase()

        // Then
        verify { alarmRepository.isBatteryOptimizationIgnored() }
        verify { alarmRepository.openBatteryOptimizationSettings() }
    }

    @Test
    fun `invoke should NOT open battery optimization settings when battery optimization IS ignored`() {
        // Given
        every { alarmRepository.isBatteryOptimizationIgnored() } returns true
        // We don't need to mock openBatteryOptimizationSettings() here as it shouldn't be called

        // When
        checkAndRequestBatteryOptimizationUseCase()

        // Then
        verify { alarmRepository.isBatteryOptimizationIgnored() }
        // Verify that openBatteryOptimizationSettings() was NOT called
        verify(exactly = 0) { alarmRepository.openBatteryOptimizationSettings() }
    }

    @Test
    fun `invoke should call isBatteryOptimizationIgnored on repository exactly once`() {
        // Given
        // The return value of isBatteryOptimizationIgnored determines the flow,
        // so we need to mock it, but the specific value doesn't matter for this interaction test.
        every { alarmRepository.isBatteryOptimizationIgnored() } returns false
        justRun { alarmRepository.openBatteryOptimizationSettings() } // Mock dependent call if the flow reaches it

        // When
        checkAndRequestBatteryOptimizationUseCase()

        // Then
        verify(exactly = 1) { alarmRepository.isBatteryOptimizationIgnored() }
    }
}
