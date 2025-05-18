package com.wedgess.luas.domain // Or com.wedgess.luas.domain if that's your test structure

import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.IsLocationPermissionIgnoredUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery // Use coEvery for suspending functions or functions returning Flow
import io.mockk.coVerify // Use coVerify for verifying suspending functions
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IsLocationPermissionIgnoredUseCaseTest {

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var isLocationPermissionIgnoredUseCase: IsLocationPermissionIgnoredUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        isLocationPermissionIgnoredUseCase = IsLocationPermissionIgnoredUseCase(preferencesRepository)
    }

    @Test
    fun `invoke should return true when repository ignoreLocationPermission flow emits true`() = runTest {
        // Given
        // Mock the repository to return a Flow that emits true
        coEvery { preferencesRepository.ignoreLocationPermission() } returns flowOf(true)

        // When
        val result = isLocationPermissionIgnoredUseCase()

        // Then
        coVerify { preferencesRepository.ignoreLocationPermission() }
        assertTrue(result)
    }

    @Test
    fun `invoke should return false when repository ignoreLocationPermission flow emits false`() = runTest {
        // Given
        // Mock the repository to return a Flow that emits false
        coEvery { preferencesRepository.ignoreLocationPermission() } returns flowOf(false)

        // When
        val result = isLocationPermissionIgnoredUseCase()

        // Then
        coVerify { preferencesRepository.ignoreLocationPermission() }
        assertFalse(result)
    }

    @Test
    fun `invoke should call ignoreLocationPermission on repository exactly once`() = runTest {
        // Given
        // The emitted value doesn't matter for this specific verification,
        // but the function needs to be mocked as it's called.
        coEvery { preferencesRepository.ignoreLocationPermission() } returns flowOf(true) // or false

        // When
        isLocationPermissionIgnoredUseCase()

        // Then
        coVerify(exactly = 1) { preferencesRepository.ignoreLocationPermission() }
    }
}
