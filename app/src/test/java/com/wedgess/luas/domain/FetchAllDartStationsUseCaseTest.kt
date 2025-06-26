package com.wedgess.luas.domain

import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.usecase.FetchAllDartStationsUseCase
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

class FetchAllDartStationsUseCaseTest {

    @MockK
    private lateinit var dartRepository: DartRepository
    private lateinit var fetchAllDartStationsUseCase: FetchAllDartStationsUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        fetchAllDartStationsUseCase = FetchAllDartStationsUseCase(dartRepository)
    }

    @Test
    fun `invoke should return repository result when successful`() = runTest {
        // Given
        val mockStations = listOf(
            DartStationEntity(
                id = 1,
                name = "St. Stephens Green",
                code = "STA",
                alias = "STA",
                latitude = 53.33963,
                longitude = -6.26070,
            ),
            DartStationEntity(
                id = 1,
                name = "Harcourt",
                code = "HAR",
                alias = "HAR",
                latitude = 53.33334,
                longitude = -6.26302,
            ),
        )
        val successResult = Result.success(mockStations)
        every { dartRepository.fetchStations() } returns flowOf(successResult)

        // When
        val result = fetchAllDartStationsUseCase().first()

        // Then
        verify { dartRepository.fetchStations() }
        assertTrue(result.isSuccess)
        assertEquals(mockStations, result.getOrNull())
    }

    @Test
    fun `invoke should return repository error when failure occurs`() = runTest {
        // Given
        val exception = Exception("Network error")
        val errorResult = Result.failure<List<DartStationEntity>>(exception)
        every { dartRepository.fetchStations() } returns flowOf(errorResult)

        // When
        val result = fetchAllDartStationsUseCase().first()

        // Then
        verify { dartRepository.fetchStations() }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
