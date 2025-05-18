package com.wedgess.luas.domain

import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.UpdateLocationPermissionRequestedUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateLocationPermissionRequestedUseCaseTest {

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var updateLocationPermissionRequestedUseCase: UpdateLocationPermissionRequestedUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        updateLocationPermissionRequestedUseCase = UpdateLocationPermissionRequestedUseCase(preferencesRepository)
    }

    @Test
    fun `invoke should call repository updateLocationPermissionRequested with true and return success`() = runTest {
        // Given
        val requested = true
        val successResult = Result.success(Unit)
        coEvery { preferencesRepository.updateLocationPermissionRequested(requested) } returns successResult

        // When
        val result = updateLocationPermissionRequestedUseCase(requested)

        // Then
        coVerify { preferencesRepository.updateLocationPermissionRequested(requested) }
        assertTrue(result.isSuccess)
        assertEquals(successResult, result)
    }

    @Test
    fun `invoke should call repository updateLocationPermissionRequested with false and return success`() = runTest {
        // Given
        val requested = false
        val successResult = Result.success(Unit)
        coEvery { preferencesRepository.updateLocationPermissionRequested(requested) } returns successResult

        // When
        val result = updateLocationPermissionRequestedUseCase(requested)

        // Then
        coVerify { preferencesRepository.updateLocationPermissionRequested(requested) }
        assertTrue(result.isSuccess)
        assertEquals(successResult, result)
    }

    @Test
    fun `invoke should return failure when repository updateLocationPermissionRequested fails`() = runTest {
        // Given
        val requested = true
        val exception = RuntimeException("Update failed")
        val failureResult = Result.failure<Unit>(exception)
        coEvery { preferencesRepository.updateLocationPermissionRequested(requested) } returns failureResult

        // When
        val result = updateLocationPermissionRequestedUseCase(requested)

        // Then
        coVerify { preferencesRepository.updateLocationPermissionRequested(requested) }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `invoke should call repository updateLocationPermissionRequested exactly once`() = runTest {
        // Given
        val requested = false // Value of requested doesn't matter for this specific verification
        coEvery { preferencesRepository.updateLocationPermissionRequested(requested) } returns Result.success(Unit)

        // When
        updateLocationPermissionRequestedUseCase(requested)

        // Then
        coVerify(exactly = 1) { preferencesRepository.updateLocationPermissionRequested(requested) }
    }
}
