package com.wedgess.luas.domain

import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.UpdateSelectedTransportTypeUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateSelectedTransportTypeUseCaseTest {

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var updateSelectedTransportTypeUseCase: UpdateSelectedTransportTypeUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        updateSelectedTransportTypeUseCase = UpdateSelectedTransportTypeUseCase(preferencesRepository)
    }

    @Test
    fun `should update transport type when invoked`() = runTest {
        // Given
        val transportType = TransportType.DART
        val successResult = Result.success(Unit)
        coEvery { preferencesRepository.updateSelectedTransportType(any()) } returns successResult

        // When
        val result = updateSelectedTransportTypeUseCase(transportType)

        // Then
        coVerify { preferencesRepository.updateSelectedTransportType(transportType) }
        assertTrue(result.isSuccess)
        assertEquals(successResult, result)
    }

    @Test
    fun `should return failure result when invoke fails`() = runTest {
        // Given
        val transportType = TransportType.LUAS
        val exception = RuntimeException("Update failed")
        val failureResult = Result.failure<Unit>(exception)
        coEvery { preferencesRepository.updateSelectedTransportType(any()) } returns failureResult

        // When
        val result = updateSelectedTransportTypeUseCase(transportType)

        // Then
        coVerify { preferencesRepository.updateSelectedTransportType(transportType) }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
