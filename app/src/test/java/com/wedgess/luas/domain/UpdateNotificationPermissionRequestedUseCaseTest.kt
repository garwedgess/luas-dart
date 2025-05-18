package com.wedgess.luas.domain

import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.UpdateNotificationPermissionRequestedUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateNotificationPermissionRequestedUseCaseTest {

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var updateNotificationPermissionRequestedUseCase: UpdateNotificationPermissionRequestedUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        updateNotificationPermissionRequestedUseCase =
            UpdateNotificationPermissionRequestedUseCase(preferencesRepository)
    }

    @Test
    fun `invoke should call repository updateNotificationPermissionRequested with true and return success`() = runTest {
        // Given
        val granted = true
        val successResult = Result.success(Unit)
        coEvery { preferencesRepository.updateNotificationPermissionRequested(granted) } returns successResult

        // When
        val result = updateNotificationPermissionRequestedUseCase(granted)

        // Then
        coVerify { preferencesRepository.updateNotificationPermissionRequested(granted) }
        assertTrue(result.isSuccess)
        assertEquals(successResult, result)
    }

    @Test
    fun `invoke should call repository updateNotificationPermissionRequested with false and return success`() =
        runTest {
            // Given
            val granted = false
            val successResult = Result.success(Unit)
            coEvery { preferencesRepository.updateNotificationPermissionRequested(granted) } returns successResult

            // When
            val result = updateNotificationPermissionRequestedUseCase(granted)

            // Then
            coVerify { preferencesRepository.updateNotificationPermissionRequested(granted) }
            assertTrue(result.isSuccess)
            assertEquals(successResult, result)
        }

    @Test
    fun `invoke should return failure when repository updateNotificationPermissionRequested fails`() = runTest {
        // Given
        val granted = true
        val exception = RuntimeException("Update failed")
        val failureResult = Result.failure<Unit>(exception)
        coEvery { preferencesRepository.updateNotificationPermissionRequested(granted) } returns failureResult

        // When
        val result = updateNotificationPermissionRequestedUseCase(granted)

        // Then
        coVerify { preferencesRepository.updateNotificationPermissionRequested(granted) }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `invoke should call repository updateNotificationPermissionRequested exactly once`() = runTest {
        // Given
        val granted = true // Value of granted doesn't matter for this specific verification
        coEvery { preferencesRepository.updateNotificationPermissionRequested(granted) } returns Result.success(Unit)

        // When
        updateNotificationPermissionRequestedUseCase(granted)

        // Then
        coVerify(exactly = 1) { preferencesRepository.updateNotificationPermissionRequested(granted) }
    }
}
