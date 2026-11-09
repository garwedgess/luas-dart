package com.wedgess.luas.data.repository

import androidx.datastore.core.DataStore
import app.cash.turbine.test
import com.wedgess.luas.data.db.dao.SelectedStationDao
import com.wedgess.luas.data.model.UserPreferences
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class PreferencesRepositoryImplTest {

    @MockK
    private lateinit var preferences: DataStore<UserPreferences>
    private lateinit var selectedStations: SelectedStationDao
    private lateinit var repository: PreferencesRepositoryImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        selectedStations = mockk(relaxed = true)
        every { selectedStations.observe(any()) } returns flowOf(null)
        repository = PreferencesRepositoryImpl(preferences, selectedStations)
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
    fun `updateSelectedRedLineStation - stores the selection in its own slot`() = runTest {
        val result = repository.updateSelectedRedLineStation("NEW_RED_STATION")

        assertTrue(result.isSuccess)
        verify { selectedStations.save("LUAS_RED", "NEW_RED_STATION") }
        coVerify(exactly = 0) { preferences.updateData(any()) }
    }

    @Test
    fun `updateSelectedRedLineStation - handles failure`() = runTest {
        val exception = RuntimeException("Test exception")
        every { selectedStations.save(any(), any()) } throws exception

        val result = repository.updateSelectedRedLineStation("NEW_RED_STATION")

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `updateSelectedGreenLineStation - stores the selection in its own slot`() = runTest {
        val result = repository.updateSelectedGreenLineStation("NEW_GREEN_STATION")

        assertTrue(result.isSuccess)
        verify { selectedStations.save("LUAS_GREEN", "NEW_GREEN_STATION") }
    }

    @Test
    fun `updateSelectedGreenLineStation - handles failure`() = runTest {
        val exception = RuntimeException("Test exception")
        every { selectedStations.save(any(), any()) } throws exception

        val result = repository.updateSelectedGreenLineStation("NEW_GREEN_STATION")

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `updateSelectedDartStation - stores the selection in its own slot`() = runTest {
        val result = repository.updateSelectedDartStation("Dublin Pearse")

        assertTrue(result.isSuccess)
        verify { selectedStations.save("DART", "Dublin Pearse") }
    }

    @Test
    fun `fetchSelectedDartStation - prefers the stored selection over the legacy DataStore value`() = runTest {
        every { selectedStations.observe("DART") } returns flowOf("Dublin Pearse")
        coEvery { preferences.data } returns
            MutableStateFlow(UserPreferences.newBuilder().setSelectedDartStation("Bray").build())

        repository.fetchSelectedDartStation().test {
            assertEquals("Dublin Pearse", awaitItem())
            cancelAndConsumeRemainingEvents()
        }
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
