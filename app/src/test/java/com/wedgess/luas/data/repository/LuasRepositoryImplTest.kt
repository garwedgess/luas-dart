package com.wedgess.luas.data.repository

import com.wedgess.luas.Stop
import com.wedgess.luas.data.api.LuasForecastApiService
import com.wedgess.luas.data.api.LuasStopApiService
import com.wedgess.luas.data.db.dao.StopsDao
import com.wedgess.luas.data.mapper.fromEntity
import com.wedgess.luas.data.mapper.toDao
import com.wedgess.luas.data.mapper.toEntity
import com.wedgess.luas.data.model.DirectionKeyData
import com.wedgess.luas.data.model.LuasLineData
import com.wedgess.luas.data.model.StopForcastResponseData
import com.wedgess.luas.data.model.StopsResponseData
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.StopEntity
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

@ExperimentalCoroutinesApi
class LuasRepositoryImplTest {

    @MockK
    private lateinit var stopsApi: LuasStopApiService

    @MockK
    private lateinit var forecastApi: LuasForecastApiService

    @RelaxedMockK
    private lateinit var stopsDao: StopsDao

    private lateinit var repository: LuasRepositoryImpl

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = LuasRepositoryImpl(
            stopsApi = stopsApi,
            forecastApi = forecastApi,
            stopsDao = stopsDao
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `fetchStops should return stops from local database when available`() = runTest {
        val line = LuasLineEntity.RED
        val dbLine = line.fromEntity()
        val dbStops = listOf(
            Stop(
                Id = UUID.randomUUID(),
                Name = "Stop 1",
                Abbreviation = "S1",
                Line = dbLine,
                Latitude = 53.0,
                Longitude = -6.0,
                IsParkRide = false,
                IsCycleRide = false
            ),
            Stop(
                Id = UUID.randomUUID(),
                Name = "Stop 2",
                Abbreviation = "S2",
                Line = dbLine,
                Latitude = 53.1,
                Longitude = -6.1,
                IsParkRide = true,
                IsCycleRide = true
            )
        )
        val expectedStops = dbStops.map { it.toEntity() }
        every { stopsDao.fetchAllByLine(dbLine) } returns flowOf(dbStops)

        val result = repository.fetchStops(line).first()

        assertTrue(result.isSuccess)
        assertEquals(expectedStops, result.getOrNull())
        verify(exactly = 1) { stopsDao.fetchAllByLine(dbLine) }
        coVerify(exactly = 0) { stopsApi.fetchStops() }
    }

    @Test
    fun `fetchStops should fetch from remote when local database is empty`() = runTest {
        val line = LuasLineEntity.RED
        val dbLine = line.fromEntity()
        val emptyList = emptyList<Stop>()
        val lineResponse = StopsResponseData(
            line = listOf(
                StopsResponseData.LineData(
                    name = LuasLineData.GREEN,
                    stop = listOf(
                        StopsResponseData.LineData.StopData(
                            name = "Stop 1",
                            pronunciation = "Stop 1",
                            abrev = "S1",
                            lat = 53.0,
                            long = -6.0,
                            isParkRide = 0,
                            isCycleRide = 0
                        ),
                        StopsResponseData.LineData.StopData(
                            name = "Stop 2",
                            pronunciation = "Stop 2",
                            abrev = "S2",
                            lat = 53.1,
                            long = -6.1,
                            isParkRide = 1,
                            isCycleRide = 1
                        )
                    )
                )
            )
        )
        every { stopsDao.fetchAllByLine(dbLine) } returns flowOf(emptyList)
        coEvery { stopsApi.fetchStops() } returns Result.success(lineResponse)

        val daoStops = lineResponse.toDao()
        coEvery { stopsDao.insert(daoStops) } just Runs

        val result = repository.fetchStops(line).first()

        assertTrue(result.isSuccess)
        verify(exactly = 1) { stopsDao.fetchAllByLine(dbLine) }
        coVerify(exactly = 1) { stopsApi.fetchStops() }
        coVerify(exactly = 1) { stopsDao.insert(stops = any()) }
    }

    @Test
    fun `fetchStops should handle remote fetch error gracefully`() = runTest {
        val line = LuasLineEntity.RED
        val dbLine = line.fromEntity()
        val exception = RuntimeException("Network error")
        val expectedStops = emptyList<StopEntity>()
        every { stopsDao.fetchAllByLine(dbLine) } returns flowOf(emptyList())
        coEvery { stopsApi.fetchStops() } returns Result.failure(exception)

        val result = repository.fetchStops(line).first()

        assertTrue(result.isSuccess)
        assertEquals(expectedStops, result.getOrNull())
        verify(exactly = 1) { stopsDao.fetchAllByLine(dbLine) }
        coVerify(exactly = 1) { stopsApi.fetchStops() }
        coVerify(exactly = 0) { stopsDao.insert(stops = any()) }
    }

    @Test
    fun `fetchAllStops should return all stops from database`() = runTest {
        val dbStops = listOf(
            Stop(
                Id = UUID.randomUUID(),
                Name = "Stop 1",
                Abbreviation = "S1",
                Line = LuasLineData.RED,
                Latitude = 53.0,
                Longitude = -6.0,
                IsParkRide = false,
                IsCycleRide = false
            ),
            Stop(
                Id = UUID.randomUUID(),
                Name = "Stop 2",
                Abbreviation = "S2",
                Line = LuasLineData.GREEN,
                Latitude = 53.1,
                Longitude = -6.1,
                IsParkRide = true,
                IsCycleRide = true
            )
        )
        val expectedStops = dbStops.map { it.toEntity() }

        every { stopsDao.fetchAll() } returns flowOf(dbStops)

        val result = repository.fetchAllStops().first()

        assertTrue(result.isSuccess)
        assertEquals(expectedStops, result.getOrNull())
        verify(exactly = 1) { stopsDao.fetchAll() }
    }

    @Test
    fun `fetchForecast should return forecast for stop`() = runTest {
        val stopAbv = "S1"
        val forecastResponse = StopForcastResponseData(
            created = "2025-03-02T21:48:50",
            stop = "St Stephens Green",
            stopAbv = "STS",
            message = "All services running normally",
            direction = listOf(
                StopForcastResponseData.DirectionData(
                    name = DirectionKeyData.INBOUND,
                    tram = emptyList()
                ),
                StopForcastResponseData.DirectionData(
                    name = DirectionKeyData.OUTBOUND,
                    tram = emptyList()
                )
            )
        )
        val expectedForecast = forecastResponse.toEntity()
        coEvery { forecastApi.fetchForecast(stopAbv) } returns Result.success(forecastResponse)

        val result = repository.fetchForecast(stopAbv)

        assertTrue(result.isSuccess)
        assertEquals(expectedForecast, result.getOrNull())
        coVerify(exactly = 1) { forecastApi.fetchForecast(stopAbv) }
    }

    @Test
    fun `fetchForecast should handle error from API`() = runTest {
        val stopAbv = "S1"
        val exception = RuntimeException("Network error")
        coEvery { forecastApi.fetchForecast(stopAbv) } returns Result.failure(exception)

        val result = repository.fetchForecast(stopAbv)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { forecastApi.fetchForecast(stopAbv) }
    }
}
