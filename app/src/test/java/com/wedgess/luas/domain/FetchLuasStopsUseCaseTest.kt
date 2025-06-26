package com.wedgess.luas.domain

import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.usecase.FetchLuasStopsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class FetchLuasStopsUseCaseTest {

    private lateinit var luasRepository: LuasRepository
    private lateinit var fetchLuasStopsUseCase: FetchLuasStopsUseCase

    @Before
    fun setup() {
        luasRepository = mockk()
        fetchLuasStopsUseCase = FetchLuasStopsUseCase(luasRepository)
    }

    @Test
    fun `invoke should return stops for given line when successful`() = runTest {
        // Given
        val line = LuasLineEntity.GREEN
        val mockStops = listOf(
            LuasStopEntity(
                id = UUID.randomUUID(),
                abbreviation = "STA",
                name = "St. Stephens Green",
                latitude = 53.33963,
                longitude = -6.26070,
                line = LuasLineEntity.GREEN,
                isParkAndRide = false,
                isCycleAndRide = false
            ),
            LuasStopEntity(
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
        every { luasRepository.fetchStops(line) } returns flowOf(successResult)

        // When
        val result = fetchLuasStopsUseCase(line).first()

        // Then
        verify { luasRepository.fetchStops(line) }
        assertTrue(result.isSuccess)
        assertEquals(mockStops, result.getOrNull())
    }

    @Test
    fun `invoke should return empty list when successful but no stops found`() = runTest {
        // Given
        val line = LuasLineEntity.RED
        val emptyStops = emptyList<LuasStopEntity>()
        val successResult = Result.success(emptyStops)
        every { luasRepository.fetchStops(line) } returns flowOf(successResult)

        // When
        val result = fetchLuasStopsUseCase(line).first()

        // Then
        verify { luasRepository.fetchStops(line) }
        assertTrue(result.isSuccess)
        assertEquals(emptyStops, result.getOrNull())
    }

    @Test
    fun `invoke should return error when repository returns failure`() = runTest {
        // Given
        val line = LuasLineEntity.GREEN
        val exception = Exception("Network error")
        val errorResult = Result.failure<List<LuasStopEntity>>(exception)
        every { luasRepository.fetchStops(line) } returns flowOf(errorResult)

        // When
        val result = fetchLuasStopsUseCase(line).first()

        // Then
        verify { luasRepository.fetchStops(line) }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
