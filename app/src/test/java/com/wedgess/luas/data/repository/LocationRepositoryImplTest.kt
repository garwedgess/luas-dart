package com.wedgess.luas.data.repository

import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.Task
import com.wedgess.luas.domain.model.UserLocation
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class LocationRepositoryImplTest {

    @MockK
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationRepository: LocationRepositoryImpl
    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockTask: Task<Void> = mockk(relaxed = true)
    private val mockLooper: Looper = mockk()
    private val locationCallbackSlot = slot<LocationCallback>()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        setupMocks()
        locationRepository = LocationRepositoryImpl(fusedLocationClient, mockLooper, testDispatcher)
    }

    @After
    fun teardown() {
        unmockkStatic(Looper::class)
    }

    @Test
    fun `should emit location updates when available`() = runTest {
        // Given
        val expectedLocations = listOf(
            UserLocation(53.349805, -6.26031),
            UserLocation(53.350000, -6.261000)
        )
        val mockLocations = expectedLocations.map {
            createMockLocation(it.latitude, it.longitude)
        }

        // When
        val results = mutableListOf<UserLocation>()
        val job = launch {
            locationRepository.getCurrentLocation().take(2).collect { results.add(it) }
        }

        // Then
        advanceUntilIdle()
        verifyLocationUpdatesRequested()

        // Simulate location updates
        mockLocations.forEach { location ->
            locationCallbackSlot.captured.onLocationResult(createMockLocationResult(location))
        }

        advanceUntilIdle()
        assertEquals(expectedLocations, results)
        job.cancel()
    }

    @Test
    fun `should emit default location when security exception occurs`() = runTest {
        // Given
        every {
            fusedLocationClient.requestLocationUpdates(
                any<LocationRequest>(),
                any<LocationCallback>(),
                any<Looper>()
            )
        } throws SecurityException("Permission denied")

        // When
        val userLocation = locationRepository.getCurrentLocation().first()

        // Then
        assertEquals(UserLocation(0.0, 0.0), userLocation)
    }

    @Test
    fun `should handle null location and emit valid location when available`() = runTest {
        // Given
        val expectedLocation = UserLocation(53.1234, -6.5678)

        // When
        val results = mutableListOf<UserLocation>()
        val job = launch {
            locationRepository.getCurrentLocation().take(1).collect { results.add(it) }
        }

        // Then
        advanceUntilIdle()
        verifyLocationUpdatesRequested()

        // Simulate null location followed by valid location
        locationCallbackSlot.captured.onLocationResult(createMockLocationResult(null))
        locationCallbackSlot.captured.onLocationResult(
            createMockLocationResult(createMockLocation(expectedLocation.latitude, expectedLocation.longitude))
        )

        advanceUntilIdle()
        assertEquals(listOf(expectedLocation), results)
        job.cancel()
    }

    @Test
    fun `should emit default location when runtime exception occurs`() = runTest {
        // Given
        every {
            fusedLocationClient.requestLocationUpdates(
                any<LocationRequest>(),
                any<LocationCallback>(),
                any<Looper>()
            )
        } throws RuntimeException("Test exception")

        // When
        val userLocation = locationRepository.getCurrentLocation().first()

        // Then
        assertEquals(UserLocation(0.0, 0.0), userLocation)
    }

    // Helper methods
    private fun setupMocks() {
        // Mock location update requests
        every {
            fusedLocationClient.requestLocationUpdates(
                any<LocationRequest>(),
                any<LocationCallback>(),
                any<Looper>()
            )
        } answers {
            secondArg<LocationCallback>().let { locationCallbackSlot.captured = it }
            mockTask
        }

        every {
            fusedLocationClient.removeLocationUpdates(any<LocationCallback>())
        } returns mockTask

        // Mock Looper
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockLooper
    }

    private fun verifyLocationUpdatesRequested() {
        verify {
            fusedLocationClient.requestLocationUpdates(
                any<LocationRequest>(),
                any<LocationCallback>(),
                any<Looper>()
            )
        }
    }

    private fun createMockLocation(lat: Double, lon: Double) = mockk<Location> {
        every { latitude } returns lat
        every { longitude } returns lon
    }

    private fun createMockLocationResult(location: Location?) = mockk<LocationResult> {
        every { lastLocation } returns location
    }
}
