package com.wedgess.luas.domain

import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.WasLocationPermissionRequestedUseCase
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
import java.util.NoSuchElementException

class WasLocationPermissionRequestedUseCaseTest {

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var wasLocationPermissionRequestedUseCase: WasLocationPermissionRequestedUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        wasLocationPermissionRequestedUseCase = WasLocationPermissionRequestedUseCase(preferencesRepository)
    }

    @Test
    fun `invoke should return true when repository returns flow of true`() = runTest {
        // Given
        coEvery { preferencesRepository.wasLocationPermissionRequested() } returns flowOf(true)

        // When
        val result = wasLocationPermissionRequestedUseCase()

        // Then
        coVerify { preferencesRepository.wasLocationPermissionRequested() }
        assertTrue(result)
    }

    @Test
    fun `invoke should return false when repository returns flow of false`() = runTest {
        // Given
        coEvery { preferencesRepository.wasLocationPermissionRequested() } returns flowOf(false)

        // When
        val result = wasLocationPermissionRequestedUseCase()

        // Then
        coVerify { preferencesRepository.wasLocationPermissionRequested() }
        assertFalse(result)
    }

    @Test
    fun `invoke should return the first element when repository flow emits multiple values`() = runTest {
        // Given
        coEvery { preferencesRepository.wasLocationPermissionRequested() } returns flowOf(true, false, false)

        // When
        val result = wasLocationPermissionRequestedUseCase()

        // Then
        coVerify { preferencesRepository.wasLocationPermissionRequested() }
        assertTrue(result) // Should be the first emitted value
    }

    @Test
    fun `invoke should call repository wasLocationPermissionRequested exactly once`() = runTest {
        // Given
        coEvery { preferencesRepository.wasLocationPermissionRequested() } returns flowOf(true)

        // When
        wasLocationPermissionRequestedUseCase()

        // Then
        coVerify(exactly = 1) { preferencesRepository.wasLocationPermissionRequested() }
    }

    @Test(expected = NoSuchElementException::class)
    fun `invoke should throw NoSuchElementException when repository returns an empty flow`() = runTest {
        // Given
        coEvery { preferencesRepository.wasLocationPermissionRequested() } returns emptyFlow()

        // When
        wasLocationPermissionRequestedUseCase()

        // Then
        // Exception expected, as defined by @Test(expected = ...)
    }

    @Test
    fun `invoke should propagate exceptions from repository`() = runTest {
        // Given
        val expectedException = RuntimeException("Repository error")
        coEvery { preferencesRepository.wasLocationPermissionRequested() } throws expectedException

        // When
        var actualException: Throwable? = null
        try {
            wasLocationPermissionRequestedUseCase()
        } catch (e: Throwable) {
            actualException = e
        }

        // Then
        coVerify { preferencesRepository.wasLocationPermissionRequested() }
        assertEquals(expectedException, actualException)
    }
}
