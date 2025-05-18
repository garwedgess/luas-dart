package com.wedgess.luas.domain

import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.UpdateIgnoreNotificationPermissionUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateIgnoreNotificationPermissionUseCaseTest {

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var updateIgnoreNotificationPermissionUseCase: UpdateIgnoreNotificationPermissionUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        updateIgnoreNotificationPermissionUseCase = UpdateIgnoreNotificationPermissionUseCase(preferencesRepository)
    }

    @Test
    fun `invoke should call repository updateIgnoreNotificationPermission with true and return success`() = runTest {
        // Given
        val ignore = true
        val successResult = Result.success(Unit)
        coEvery { preferencesRepository.updateIgnoreNotificationPermission(ignore) } returns successResult

        // When
        val result = updateIgnoreNotificationPermissionUseCase(ignore)

        // Then
        coVerify { preferencesRepository.updateIgnoreNotificationPermission(ignore) }
        assertTrue(result.isSuccess)
        assertEquals(successResult, result)
    }

    @Test
    fun `invoke should call repository updateIgnoreNotificationPermission with false and return success`() = runTest {
        // Given
        val ignore = false
        val successResult = Result.success(Unit)
        coEvery { preferencesRepository.updateIgnoreNotificationPermission(ignore) } returns successResult

        // When
        val result = updateIgnoreNotificationPermissionUseCase(ignore)

        // Then
        coVerify { preferencesRepository.updateIgnoreNotificationPermission(ignore) }
        assertTrue(result.isSuccess)
        assertEquals(successResult, result)
    }

    @Test
    fun `invoke should return failure when repository updateIgnoreNotificationPermission fails`() = runTest {
        // Given
        val ignore = true
        val exception = RuntimeException("Update failed")
        val failureResult = Result.failure<Unit>(exception)
        coEvery { preferencesRepository.updateIgnoreNotificationPermission(ignore) } returns failureResult

        // When
        val result = updateIgnoreNotificationPermissionUseCase(ignore)

        // Then
        coVerify { preferencesRepository.updateIgnoreNotificationPermission(ignore) }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `invoke should call repository updateIgnoreNotificationPermission exactly once`() = runTest {
        // Given
        val ignore = false
        coEvery { preferencesRepository.updateIgnoreNotificationPermission(ignore) } returns Result.success(Unit)

        // When
        updateIgnoreNotificationPermissionUseCase(ignore)

        // Then
        coVerify(exactly = 1) { preferencesRepository.updateIgnoreNotificationPermission(ignore) }
    }
}
