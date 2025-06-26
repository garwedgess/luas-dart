package com.wedgess.luas.domain

import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.UpdateSelectedDartStationUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateSelectedDartStationUseCaseTest {

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var updateSelectedDartStationUseCase: UpdateSelectedDartStationUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        updateSelectedDartStationUseCase = UpdateSelectedDartStationUseCase(preferencesRepository)
    }

    @Test
    fun `should update station when invoked`() = runTest {
        // Given
        val station = "GSTNS"
        val successResult = Result.success(Unit)
        coEvery { preferencesRepository.updateSelectedDartStation(any()) } returns successResult

        // When
        val result = updateSelectedDartStationUseCase(station)

        // Then
        coVerify { preferencesRepository.updateSelectedDartStation(station) }
        assertTrue(result.isSuccess)
        assertEquals(successResult, result)
    }

    @Test
    fun `should return failure result when invoking fails`() = runTest {
        // Given
        val station = "TAL"
        val exception = RuntimeException("Update failed")
        val failureResult = Result.failure<Unit>(exception)
        coEvery { preferencesRepository.updateSelectedDartStation(any()) } returns failureResult

        // When
        val result = updateSelectedDartStationUseCase(station)

        // Then
        coVerify { preferencesRepository.updateSelectedDartStation(station) }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
