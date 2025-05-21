package com.wedgess.luas.data.repository

import androidx.datastore.core.DataStore
import app.cash.turbine.test
import com.wedgess.luas.data.model.UserPreferences
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class PreferencesRepositoryImplTest {

    @MockK
    private lateinit var preferences: DataStore<UserPreferences>
    private lateinit var repository: PreferencesRepositoryImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = PreferencesRepositoryImpl(preferences)
    }

    @Test
    fun `fetchSelectedRedLineStation - returns the correct station`() = runTest {
        // Given
        val userPreferencesFlow = MutableStateFlow(
            UserPreferences.newBuilder()
                .setSelectedRedLineStation("RED_STATION")
                .setSelectedGreenLineStation("GREEN_STATION")
                .build()
        )
        coEvery { preferences.data } returns userPreferencesFlow

        // When/Then
        repository.fetchSelectedRedLineStation().test {
            val station = awaitItem()
            assertEquals("RED_STATION", station)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `fetchSelectedGreenLineStation - returns the correct station`() = runTest {
        // Given
        val userPreferencesFlow = MutableStateFlow(
            UserPreferences.newBuilder()
                .setSelectedRedLineStation("RED_STATION")
                .setSelectedGreenLineStation("GREEN_STATION")
                .build()
        )
        coEvery { preferences.data } returns userPreferencesFlow

        // When/Then
        repository.fetchSelectedGreenLineStation().test {
            val station = awaitItem()
            assertEquals("GREEN_STATION", station)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `updateSelectedRedLineStation - updates the station successfully`() = runTest {
        // Given
        val userPreferencesFlow = MutableStateFlow(
            UserPreferences.newBuilder()
                .setSelectedRedLineStation("OLD_RED_STATION")
                .setSelectedGreenLineStation("GREEN_STATION")
                .build()
        )
        coEvery { preferences.data } returns userPreferencesFlow

        val updateDataLambdaSlot = slot<suspend (UserPreferences) -> UserPreferences>()
        coEvery { preferences.updateData(capture(updateDataLambdaSlot)) } coAnswers {
            val lambda = updateDataLambdaSlot.captured
            userPreferencesFlow.value = lambda(userPreferencesFlow.value)
            userPreferencesFlow.value
        }

        // When
        val result = repository.updateSelectedRedLineStation("NEW_RED_STATION")

        // Then
        assertTrue(result.isSuccess)
        coVerify { preferences.updateData(any()) }
        assertEquals("NEW_RED_STATION", userPreferencesFlow.value.selectedRedLineStation)
    }

    @Test
    fun `updateSelectedRedLineStation - handles failure`() = runTest {
        // Given
        val exception = RuntimeException("Update error")
        coEvery { preferences.updateData(any()) } throws exception

        // When
        val result = repository.updateSelectedRedLineStation("NEW_RED_STATION")

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify { preferences.updateData(any()) }
    }

    @Test
    fun `updateSelectedGreenLineStation - updates the station successfully`() = runTest {
        // Given
        val userPreferencesFlow = MutableStateFlow(
            UserPreferences.newBuilder()
                .setSelectedRedLineStation("RED_STATION")
                .setSelectedGreenLineStation("OLD_GREEN_STATION")
                .build()
        )
        coEvery { preferences.data } returns userPreferencesFlow

        val updateDataLambdaSlot = slot<suspend (UserPreferences) -> UserPreferences>()
        coEvery { preferences.updateData(capture(updateDataLambdaSlot)) } coAnswers {
            val lambda = updateDataLambdaSlot.captured
            userPreferencesFlow.value = lambda(userPreferencesFlow.value)
            userPreferencesFlow.value
        }

        // When
        val result = repository.updateSelectedGreenLineStation("NEW_GREEN_STATION")

        // Then
        assertTrue(result.isSuccess)
        coVerify { preferences.updateData(any()) }
        assertEquals("NEW_GREEN_STATION", userPreferencesFlow.value.selectedGreenLineStation)
    }

    @Test
    fun `updateSelectedGreenLineStation - handles failure`() = runTest {
        // Given
        val exception = RuntimeException("Update error")
        coEvery { preferences.updateData(any()) } throws exception

        // When
        val result = repository.updateSelectedGreenLineStation("NEW_GREEN_STATION")

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify { preferences.updateData(any()) }
    }

    @Test
    fun `updateLocationPermissionRequested - updates the requested state successfully`() = runTest {
        // Given
        val userPreferencesFlow = MutableStateFlow(
            UserPreferences.newBuilder()
                .setLocationPermissionRequested(false)
                .build()
        )
        coEvery { preferences.data } returns userPreferencesFlow

        val updateDataLambdaSlot = slot<suspend (UserPreferences) -> UserPreferences>()
        coEvery { preferences.updateData(capture(updateDataLambdaSlot)) } coAnswers {
            val lambda = updateDataLambdaSlot.captured
            userPreferencesFlow.value = lambda(userPreferencesFlow.value)
            userPreferencesFlow.value
        }

        // When
        val result = repository.updateLocationPermissionRequested(true)

        // Then
        assertTrue(result.isSuccess)
        coVerify { preferences.updateData(any()) }
        assertTrue(userPreferencesFlow.value.locationPermissionRequested)
    }

    @Test
    fun `updateLocationPermissionRequested - handles failure`() = runTest {
        // Given
        val exception = RuntimeException("Update error")
        coEvery { preferences.updateData(any()) } throws exception

        // When
        val result = repository.updateLocationPermissionRequested(true)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify { preferences.updateData(any()) }
    }

    @Test
    fun `updateIgnoreLocationPermission - updates the ignore state successfully`() = runTest {
        // Given
        val userPreferencesFlow = MutableStateFlow(
            UserPreferences.newBuilder()
                .setIgnoreLocationPermission(false)
                .build()
        )
        coEvery { preferences.data } returns userPreferencesFlow

        val updateDataLambdaSlot = slot<suspend (UserPreferences) -> UserPreferences>()
        coEvery { preferences.updateData(capture(updateDataLambdaSlot)) } coAnswers {
            val lambda = updateDataLambdaSlot.captured
            userPreferencesFlow.value = lambda(userPreferencesFlow.value)
            userPreferencesFlow.value
        }

        // When
        val result = repository.updateIgnoreLocationPermission(true)

        // Then
        assertTrue(result.isSuccess)
        coVerify { preferences.updateData(any()) }
        assertTrue(userPreferencesFlow.value.ignoreLocationPermission)
    }

    @Test
    fun `updateIgnoreLocationPermission - handles failure`() = runTest {
        // Given
        val exception = RuntimeException("Update error")
        coEvery { preferences.updateData(any()) } throws exception

        // When
        val result = repository.updateIgnoreLocationPermission(true)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify { preferences.updateData(any()) }
    }

    @Test
    fun `updateNotificationPermissionRequested - updates the requested state successfully`() = runTest {
        // Given
        val userPreferencesFlow = MutableStateFlow(
            UserPreferences.newBuilder()
                .setNotificationPermissionRequested(false)
                .build()
        )
        coEvery { preferences.data } returns userPreferencesFlow

        val updateDataLambdaSlot = slot<suspend (UserPreferences) -> UserPreferences>()
        coEvery { preferences.updateData(capture(updateDataLambdaSlot)) } coAnswers {
            val lambda = updateDataLambdaSlot.captured
            userPreferencesFlow.value = lambda(userPreferencesFlow.value)
            userPreferencesFlow.value
        }

        // When
        val result = repository.updateNotificationPermissionRequested(true)

        // Then
        assertTrue(result.isSuccess)
        coVerify { preferences.updateData(any()) }
        assertTrue(userPreferencesFlow.value.notificationPermissionRequested)
    }

    @Test
    fun `updateNotificationPermissionRequested - handles failure`() = runTest {
        // Given
        val exception = RuntimeException("Update error")
        coEvery { preferences.updateData(any()) } throws exception

        // When
        val result = repository.updateNotificationPermissionRequested(true)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify { preferences.updateData(any()) }
    }

    @Test
    fun `updateIgnoreNotificationPermission - updates the ignore state successfully`() = runTest {
        // Given
        val userPreferencesFlow = MutableStateFlow(
            UserPreferences.newBuilder()
                .setIgnoreNotificationPermission(false)
                .build()
        )
        coEvery { preferences.data } returns userPreferencesFlow

        val updateDataLambdaSlot = slot<suspend (UserPreferences) -> UserPreferences>()
        coEvery { preferences.updateData(capture(updateDataLambdaSlot)) } coAnswers {
            val lambda = updateDataLambdaSlot.captured
            userPreferencesFlow.value = lambda(userPreferencesFlow.value)
            userPreferencesFlow.value
        }

        // When
        val result = repository.updateIgnoreNotificationPermission(true)

        // Then
        assertTrue(result.isSuccess)
        coVerify { preferences.updateData(any()) }
        assertTrue(userPreferencesFlow.value.ignoreNotificationPermission)
    }

    @Test
    fun `updateIgnoreNotificationPermission - handles failure`() = runTest {
        // Given
        val exception = RuntimeException("Update error")
        coEvery { preferences.updateData(any()) } throws exception

        // When
        val result = repository.updateIgnoreNotificationPermission(true)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify { preferences.updateData(any()) }
    }

    @Test
    fun `ignoreLocationPermission - returns the correct state`() = runTest {
        // Given
        val userPreferencesFlow = MutableStateFlow(
            UserPreferences.newBuilder()
                .setIgnoreLocationPermission(true)
                .build()
        )
        coEvery { preferences.data } returns userPreferencesFlow

        // When/Then
        repository.ignoreLocationPermission().test {
            val ignored = awaitItem()
            assertTrue(ignored)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `ignoreNotificationPermission - returns the correct state`() = runTest {
        // Given
        val userPreferencesFlow = MutableStateFlow(
            UserPreferences.newBuilder()
                .setIgnoreNotificationPermission(true)
                .build()
        )
        coEvery { preferences.data } returns userPreferencesFlow

        // When/Then
        repository.ignoreNotificationPermission().test {
            val ignored = awaitItem()
            assertTrue(ignored)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `wasLocationPermissionRequested - returns the correct state`() = runTest {
        // Given
        val userPreferencesFlow = MutableStateFlow(
            UserPreferences.newBuilder()
                .setLocationPermissionRequested(true)
                .build()
        )
        coEvery { preferences.data } returns userPreferencesFlow

        // When/Then
        repository.wasLocationPermissionRequested().test {
            val requested = awaitItem()
            assertTrue(requested)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `wasNotificationPermissionRequested - returns the correct state`() = runTest {
        // Given
        val userPreferencesFlow = MutableStateFlow(
            UserPreferences.newBuilder()
                .setNotificationPermissionRequested(true)
                .build()
        )
        coEvery { preferences.data } returns userPreferencesFlow

        // When/Then
        repository.wasNotificationPermissionRequested().test {
            val requested = awaitItem()
            assertTrue(requested)
            cancelAndConsumeRemainingEvents()
        }
    }
}
