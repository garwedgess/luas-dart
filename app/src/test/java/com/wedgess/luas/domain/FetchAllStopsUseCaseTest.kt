package com.wedgess.luas.domain

import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.usecase.FetchAllStopsUseCase
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
import java.util.UUID

class FetchAllStopsUseCaseTest {

    @MockK
    private lateinit var luasRepository: LuasRepository
    private lateinit var fetchAllStopsUseCase: FetchAllStopsUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        fetchAllStopsUseCase = FetchAllStopsUseCase(luasRepository)
    }

    @Test
    fun `invoke should return repository result when successful`() = runTest {
        // Given
        val mockStops = listOf(
            StopEntity(
                id = UUID.randomUUID(),
                abbreviation = "STA",
                name = "St. Stephens Green",
                latitude = 53.33963,
                longitude = -6.26070,
                line = LuasLineEntity.GREEN,
                isParkAndRide = false,
                isCycleAndRide = false
            ),
            StopEntity(
                id = UUID.randomUUID(),
                abbreviation = "HAR",
                name = "Harcourt",
                latitude = 53.33334,
                longitude = -6.26302,
                line = LuasLineEntity.GREEN,
                isParkAndRide = false,
                isCycleAndRide = false
            )
        )
        val successResult = Result.success(mockStops)
        every { luasRepository.fetchAllStops() } returns flowOf(successResult)

        // When
        val result = fetchAllStopsUseCase().first()

        // Then
        verify { luasRepository.fetchAllStops() }
        assertTrue(result.isSuccess)
        assertEquals(mockStops, result.getOrNull())
    }

    @Test
    fun `invoke should return repository error when failure occurs`() = runTest {
        // Given
        val exception = Exception("Network error")
        val errorResult = Result.failure<List<StopEntity>>(exception)
        every { luasRepository.fetchAllStops() } returns flowOf(errorResult)

        // When
        val result = fetchAllStopsUseCase().first()

        // Then
        verify { luasRepository.fetchAllStops() }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
