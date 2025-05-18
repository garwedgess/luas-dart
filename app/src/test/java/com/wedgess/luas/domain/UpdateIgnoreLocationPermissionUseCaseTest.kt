package com.wedgess.luas.domain

import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.UpdateIgnoreLocationPermissionUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateIgnoreLocationPermissionUseCaseTest {

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var updateIgnoreLocationPermissionUseCase: UpdateIgnoreLocationPermissionUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        updateIgnoreLocationPermissionUseCase = UpdateIgnoreLocationPermissionUseCase(preferencesRepository)
    }

    @Test
    fun `invoke should call repository updateIgnoreLocationPermission with true and return success`() = runTest {
        // Given
        val ignore = true
        val successResult = Result.success(Unit)
        coEvery { preferencesRepository.updateIgnoreLocationPermission(ignore) } returns successResult

        // When
        val result = updateIgnoreLocationPermissionUseCase(ignore)

        // Then
        coVerify { preferencesRepository.updateIgnoreLocationPermission(ignore) }
        assertTrue(result.isSuccess)
        assertEquals(successResult, result)
    }

    @Test
    fun `invoke should call repository updateIgnoreLocationPermission with false and return success`() = runTest {
        // Given
        val ignore = false
        val successResult = Result.success(Unit)
        coEvery { preferencesRepository.updateIgnoreLocationPermission(ignore) } returns successResult

        // When
        val result = updateIgnoreLocationPermissionUseCase(ignore)

        // Then
        coVerify { preferencesRepository.updateIgnoreLocationPermission(ignore) }
        assertTrue(result.isSuccess)
        assertEquals(successResult, result)
    }

    @Test
    fun `invoke should return failure when repository updateIgnoreLocationPermission fails`() = runTest {
        // Given
        val ignore = true
        val exception = RuntimeException("Update failed")
        val failureResult = Result.failure<Unit>(exception)
        coEvery { preferencesRepository.updateIgnoreLocationPermission(ignore) } returns failureResult

        // When
        val result = updateIgnoreLocationPermissionUseCase(ignore)

        // Then
        coVerify { preferencesRepository.updateIgnoreLocationPermission(ignore) }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `invoke should call repository updateIgnoreLocationPermission exactly once`() = runTest {
        // Given
        val ignore = true
        coEvery { preferencesRepository.updateIgnoreLocationPermission(ignore) } returns Result.success(Unit)

        // When
        updateIgnoreLocationPermissionUseCase(ignore)

        // Then
        coVerify(exactly = 1) { preferencesRepository.updateIgnoreLocationPermission(ignore) }
    }
}
