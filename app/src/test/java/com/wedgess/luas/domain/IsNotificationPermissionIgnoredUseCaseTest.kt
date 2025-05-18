package com.wedgess.luas.domain

import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.IsNotificationPermissionIgnoredUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IsNotificationPermissionIgnoredUseCaseTest {

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var isNotificationPermissionIgnoredUseCase: IsNotificationPermissionIgnoredUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        isNotificationPermissionIgnoredUseCase = IsNotificationPermissionIgnoredUseCase(preferencesRepository)
    }

    @Test
    fun `invoke should return true when repository ignoreNotificationPermission flow emits true`() = runTest {
        // Given
        coEvery { preferencesRepository.ignoreNotificationPermission() } returns flowOf(true)

        // When
        val result = isNotificationPermissionIgnoredUseCase()

        // Then
        coVerify { preferencesRepository.ignoreNotificationPermission() }
        assertTrue(result)
    }

    @Test
    fun `invoke should return false when repository ignoreNotificationPermission flow emits false`() = runTest {
        // Given
        coEvery { preferencesRepository.ignoreNotificationPermission() } returns flowOf(false)

        // When
        val result = isNotificationPermissionIgnoredUseCase()

        // Then
        coVerify { preferencesRepository.ignoreNotificationPermission() }
        assertFalse(result)
    }

    @Test
    fun `invoke should call ignoreNotificationPermission on repository exactly once`() = runTest {
        // Given
        coEvery { preferencesRepository.ignoreNotificationPermission() } returns flowOf(true) // or false

        // When
        isNotificationPermissionIgnoredUseCase()

        // Then
        coVerify(exactly = 1) { preferencesRepository.ignoreNotificationPermission() }
    }
}
