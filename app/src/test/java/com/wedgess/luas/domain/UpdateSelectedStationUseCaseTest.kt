package com.wedgess.luas.domain

import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.UpdateSelectedStationUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateSelectedStationUseCaseTest {

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var updateSelectedStationUseCase: UpdateSelectedStationUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        updateSelectedStationUseCase = UpdateSelectedStationUseCase(preferencesRepository)
    }

    @Test
    fun `invoke should update red line station when RED line is provided`() = runTest {
        // Given
        val stationAbbreviation = "TAL"
        val successResult = Result.success(Unit)
        coEvery { preferencesRepository.updateSelectedRedLineStation(any()) } returns successResult

        // When
        val result = updateSelectedStationUseCase(stationAbbreviation, LuasLineEntity.RED)

        // Then
        coVerify { preferencesRepository.updateSelectedRedLineStation(stationAbbreviation) }
        assertTrue(result.isSuccess)
        assertEquals(successResult, result)
    }

    @Test
    fun `invoke should update green line station when GREEN line is provided`() = runTest {
        // Given
        val stationAbbreviation = "STI"
        val successResult = Result.success(Unit)
        coEvery { preferencesRepository.updateSelectedGreenLineStation(any()) } returns successResult

        // When
        val result = updateSelectedStationUseCase(stationAbbreviation, LuasLineEntity.GREEN)

        // Then
        coVerify { preferencesRepository.updateSelectedGreenLineStation(stationAbbreviation) }
        assertTrue(result.isSuccess)
        assertEquals(successResult, result)
    }

    @Test
    fun `invoke should return failure result when repository update for RED line fails`() = runTest {
        // Given
        val stationAbbreviation = "TAL"
        val exception = RuntimeException("Update failed")
        val failureResult = Result.failure<Unit>(exception)
        coEvery { preferencesRepository.updateSelectedRedLineStation(any()) } returns failureResult

        // When
        val result = updateSelectedStationUseCase(stationAbbreviation, LuasLineEntity.RED)

        // Then
        coVerify { preferencesRepository.updateSelectedRedLineStation(stationAbbreviation) }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `invoke should return failure result when repository update for GREEN line fails`() = runTest {
        // Given
        val stationAbbreviation = "STI"
        val exception = RuntimeException("Update failed")
        val failureResult = Result.failure<Unit>(exception)
        coEvery { preferencesRepository.updateSelectedGreenLineStation(any()) } returns failureResult

        // When
        val result = updateSelectedStationUseCase(stationAbbreviation, LuasLineEntity.GREEN)

        // Then
        coVerify { preferencesRepository.updateSelectedGreenLineStation(stationAbbreviation) }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
