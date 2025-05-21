package com.wedgess.luas.domain

import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.WasNotificationPermissionRequestedUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WasNotificationPermissionRequestedUseCaseTest {

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var wasNotificationPermissionRequestedUseCase: WasNotificationPermissionRequestedUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        wasNotificationPermissionRequestedUseCase = WasNotificationPermissionRequestedUseCase(preferencesRepository)
    }

    @Test
    fun `invoke should return true when repository returns flow of true`() = runTest {
        // Given
        coEvery { preferencesRepository.wasNotificationPermissionRequested() } returns flowOf(true)

        // When
        val result = wasNotificationPermissionRequestedUseCase()

        // Then
        coVerify { preferencesRepository.wasNotificationPermissionRequested() }
        assertTrue(result)
    }

    @Test
    fun `invoke should return false when repository returns flow of false`() = runTest {
        // Given
        coEvery { preferencesRepository.wasNotificationPermissionRequested() } returns flowOf(false)

        // When
        val result = wasNotificationPermissionRequestedUseCase()

        // Then
        coVerify { preferencesRepository.wasNotificationPermissionRequested() }
        assertFalse(result)
    }

    @Test
    fun `invoke should return the first element when repository flow emits multiple values`() = runTest {
        // Given
        // For example, if the permission was requested, then denied, then requested again (though unlikely for this specific flag)
        coEvery { preferencesRepository.wasNotificationPermissionRequested() } returns flowOf(true, false, true)

        // When
        val result = wasNotificationPermissionRequestedUseCase()

        // Then
        coVerify { preferencesRepository.wasNotificationPermissionRequested() }
        assertTrue(result) // Should be the first emitted value
    }

    @Test
    fun `invoke should call repository wasNotificationPermissionRequested exactly once`() = runTest {
        // Given
        coEvery { preferencesRepository.wasNotificationPermissionRequested() } returns flowOf(false)

        // When
        wasNotificationPermissionRequestedUseCase()

        // Then
        coVerify(exactly = 1) { preferencesRepository.wasNotificationPermissionRequested() }
    }

    @Test(expected = NoSuchElementException::class)
    fun `invoke should throw NoSuchElementException when repository returns an empty flow`() = runTest {
        // Given
        coEvery { preferencesRepository.wasNotificationPermissionRequested() } returns emptyFlow()

        // When
        wasNotificationPermissionRequestedUseCase()

        // Then
        // Exception expected, as defined by @Test(expected = ...)
        // This happens because .first() on an empty flow throws this exception.
    }

    @Test
    fun `invoke should propagate exceptions from repository when flow itself throws an exception`() = runTest {
        // Given
        val expectedException = RuntimeException("Repository error during flow collection")
        // Simulate an error occurring when the flow is constructed or collected
        coEvery { preferencesRepository.wasNotificationPermissionRequested() } throws expectedException

        // When
        var actualException: Throwable? = null
        try {
            wasNotificationPermissionRequestedUseCase()
        } catch (e: Throwable) {
            actualException = e
        }

        // Then
        coVerify { preferencesRepository.wasNotificationPermissionRequested() }
        assertEquals(expectedException, actualException)
    }
}
