package com.wedgess.luas.domain

import com.wedgess.luas.domain.model.LuasForecastEntity
import com.wedgess.luas.domain.model.RefreshMode
import com.wedgess.luas.domain.model.RefreshState
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.usecase.FetchLuasStopForecastUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class FetchLuasStopForecastUseCaseTest {

    @MockK
    private lateinit var luasRepository: LuasRepository

    private lateinit var fetchLuasStopForecastUseCase: FetchLuasStopForecastUseCase

    private val stopAbv = "STA"
    private val mockForecast = LuasForecastEntity(
        message = "Trams operating normally",
        stop = "St. Stephens Green",
        createdAt = "2025-03-02T21:48:50",
        inboundTrams = emptyList(),
        outboundTrams = emptyList(),
        stopAbv = "STS"
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { luasRepository.fetchForecast(stopAbv) } returns Result.success(mockForecast)

        fetchLuasStopForecastUseCase = FetchLuasStopForecastUseCase(luasRepository)
    }

    @Test
    fun `invoke should emit success states with increasing progress in AUTOMATIC mode`() = runTest {
        val results = mutableListOf<RefreshState<LuasForecastEntity>>()
        val job = launch { fetchLuasStopForecastUseCase(stopAbv).take(5).toList(results) }

        advanceTimeBy(5000)

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it is RefreshState.Success })
        val progressValues =
            results.filterIsInstance<RefreshState.Success<LuasForecastEntity>>().map { it.progress }
        assertTrue(progressValues.first() < progressValues.last())

        job.cancel()
    }

    @Test
    fun `invoke should emit error state when repository fails`() = runTest {
        val exception = Exception("Network error")
        coEvery { luasRepository.fetchForecast(stopAbv) } returns Result.failure(exception)

        val result = fetchLuasStopForecastUseCase(stopAbv).first()

        assertTrue(result is RefreshState.Error)
        assertEquals(exception, (result as RefreshState.Error).exception)
    }

    @Test
    fun `refresh should change refresh mode and trigger refresh flow`() = runTest {
        fetchLuasStopForecastUseCase.refresh(RefreshMode.MANUAL)
        val result = fetchLuasStopForecastUseCase(stopAbv).first()

        assertTrue(result is RefreshState.Success)
        assertEquals(
            100f,
            (result as RefreshState.Success).progress
        )
    }
}
