package com.wedgess.luas.domain

import com.wedgess.luas.domain.model.DartDirectionEntity
import com.wedgess.luas.domain.model.DartLocationTypeEntity
import com.wedgess.luas.domain.model.DartStationForecastEntity
import com.wedgess.luas.domain.model.LuasForecastEntity
import com.wedgess.luas.domain.model.RefreshMode
import com.wedgess.luas.domain.model.RefreshState
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.usecase.FetchDartStationForecastUseCase
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
class FetchDartStationForecastUseCaseTest {

    @MockK
    private lateinit var dartRepository: DartRepository

    private lateinit var fetchDartStationForecastUseCase: FetchDartStationForecastUseCase

    private val stopAbv = "STA"
    private val mockForecast = DartStationForecastEntity(
        serverTime = "2025-06-26T21:35:15.323",
        trainCode = "E949",
        stationFullName = "Greystones",
        stationCode = "GSTNS",
        queryTime = "21:35:15",
        trainDate = "26 Jun 2025",
        origin = "Greystones",
        destination = "Howth",
        originTime = "21:50",
        destinationTime = "23:15",
        status = "No Information",
        lastLocation = "",
        dueIn = 15,
        late = 0,
        expArrival = "00:00",
        expDepart = "21:50",
        schArrival = "00:00",
        schDepart = "21:50",
        direction = DartDirectionEntity.NORTHBOUND,
        trainType = "DART",
        locationType = DartLocationTypeEntity.ORIGIN,
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { dartRepository.fetchForecast(stopAbv) } returns Result.success(listOf(mockForecast))

        fetchDartStationForecastUseCase = FetchDartStationForecastUseCase(dartRepository)
    }

    @Test
    fun `invoke should emit success states with increasing progress in AUTOMATIC mode`() = runTest {
        val results = mutableListOf<RefreshState<List<DartStationForecastEntity>>>()
        val job = launch { fetchDartStationForecastUseCase(stopAbv).take(5).toList(results) }

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
        coEvery { dartRepository.fetchForecast(stopAbv) } returns Result.failure(exception)

        val result = fetchDartStationForecastUseCase(stopAbv).first()

        assertTrue(result is RefreshState.Error)
        assertEquals(exception, (result as RefreshState.Error).exception)
    }

    @Test
    fun `refresh should change refresh mode and trigger refresh flow`() = runTest {
        fetchDartStationForecastUseCase.refresh(RefreshMode.MANUAL)
        val result = fetchDartStationForecastUseCase(stopAbv).first()

        assertTrue(result is RefreshState.Success)
        assertEquals(
            100f,
            (result as RefreshState.Success).progress,
        )
    }
}
