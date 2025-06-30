package com.wedgess.luas.domain

import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.usecase.FetchAllDartStationLocationsUseCase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FetchAllDartStationLocationsUseCaseTest {

    @MockK
    private lateinit var dartRepository: DartRepository
    private lateinit var fetchAllDartStationLocationsUseCase: FetchAllDartStationLocationsUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        fetchAllDartStationLocationsUseCase = FetchAllDartStationLocationsUseCase(dartRepository)
    }

    @Test
    fun `invoke should return repository result when successful`() = runTest {
        // Given
        val mockStations = listOf(
            LocationEntity.Dart(
                name = "St. Stephens Green",
                latitude = 53.33963,
                longitude = -6.26070
            ),
            LocationEntity.Dart(
                name = "Harcourt",
                latitude = 53.33334,
                longitude = -6.26302
            )
        )
        val successResult = Result.success(mockStations)
        every { dartRepository.fetchAllStationLocations() } returns flowOf(successResult)

        // When
        val result = fetchAllDartStationLocationsUseCase().first()

        // Then
        verify { dartRepository.fetchAllStationLocations() }
        assertTrue(result.isSuccess)
        assertEquals(mockStations, result.getOrNull())
    }

    @Test
    fun `invoke should return repository error when failure occurs`() = runTest {
        // Given
        val exception = Exception("Network error")
        val errorResult = Result.failure<List<LocationEntity.Dart>>(exception)
        every { dartRepository.fetchAllStationLocations() } returns flowOf(errorResult)

        // When
        val result = fetchAllDartStationLocationsUseCase().first()

        // Then
        verify { dartRepository.fetchAllStationLocations() }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
