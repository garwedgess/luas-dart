package com.wedgess.luas.domain

import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.usecase.FetchAllLuasStopLocationsUseCase
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

class FetchAllLuasStopLocationsUseCaseTest {

    @MockK
    private lateinit var luasRepository: LuasRepository
    private lateinit var fetchAllLuasStopLocationsUseCase: FetchAllLuasStopLocationsUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        fetchAllLuasStopLocationsUseCase = FetchAllLuasStopLocationsUseCase(luasRepository)
    }

    @Test
    fun `invoke should return repository result when successful`() = runTest {
        // Given
        val mockStops = listOf(
            LocationEntity.Luas(
                name = "St. Stephens Green",
                latitude = 53.33963,
                longitude = -6.26070,
                line = LuasLineEntity.GREEN,
            ),
            LocationEntity.Luas(
                name = "Harcourt",
                latitude = 53.33334,
                longitude = -6.26302,
                line = LuasLineEntity.GREEN,
            ),
        )
        val successResult = Result.success(mockStops)
        every { luasRepository.fetchAllStopLocations() } returns flowOf(successResult)

        // When
        val result = fetchAllLuasStopLocationsUseCase().first()

        // Then
        verify { luasRepository.fetchAllStopLocations() }
        assertTrue(result.isSuccess)
        assertEquals(mockStops, result.getOrNull())
    }

    @Test
    fun `invoke should return repository error when failure occurs`() = runTest {
        // Given
        val exception = Exception("Network error")
        val errorResult = Result.failure<List<LocationEntity.Luas>>(exception)
        every { luasRepository.fetchAllStopLocations() } returns flowOf(errorResult)

        // When
        val result = fetchAllLuasStopLocationsUseCase().first()

        // Then
        verify { luasRepository.fetchAllStopLocations() }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
