package com.wedgess.luas.data.repository

import com.wedgess.luas.DartStation
import com.wedgess.luas.data.api.DartStationForecastApiService
import com.wedgess.luas.data.api.DartStationsApiService
import com.wedgess.luas.data.db.dao.DartStationDao
import com.wedgess.luas.data.mapper.toDao
import com.wedgess.luas.data.mapper.toEntity
import com.wedgess.luas.data.model.DartDirectionData
import com.wedgess.luas.data.model.DartLocationTypeData
import com.wedgess.luas.data.model.DartStationForecastResponseData
import com.wedgess.luas.data.model.DartStationForecastResponseData.DartStationForecastData
import com.wedgess.luas.data.model.DartStationsResponseData
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.repository.DartRepository
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

@ExperimentalCoroutinesApi
class DartRepositoryImplTest {

    @MockK
    private lateinit var stationsApi: DartStationsApiService

    @MockK
    private lateinit var forecastApi: DartStationForecastApiService

    @RelaxedMockK
    private lateinit var stationsDao: DartStationDao

    private lateinit var repository: DartRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = DartRepositoryImpl(
            stationsApi = stationsApi,
            forecastApi = forecastApi,
            stationsDao = stationsDao,
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `fetchStops should return stops from local database when available`() = runTest {
        val dbStops = listOf(
            DartStation(
                Id = 1,
                Name = "Stop 1",
                Code = "S1",
                Alias = "S1",
                Latitude = 53.0,
                Longitude = -6.0,
            ),
            DartStation(
                Id = 2,
                Name = "Stop 2",
                Code = "S2",
                Alias = "S2",
                Latitude = 54.0,
                Longitude = -5.0,
            ),
        )
        val expectedStops = dbStops.map { it.toEntity() }
        every { stationsDao.fetchAll() } returns flowOf(dbStops)

        val result = repository.fetchStations().first()

        assertTrue(result.isSuccess)
        assertEquals(expectedStops, result.getOrNull())
        verify(exactly = 1) { stationsDao.fetchAll() }
        coVerify(exactly = 0) { stationsApi.fetchStations() }
    }

    @Test
    fun `fetchStops should fetch from remote when local database is empty`() = runTest {
        val emptyList = emptyList<DartStation>()
        val lineResponse = DartStationsResponseData(
            stations = listOf(
                DartStationsResponseData.DartStationData(
                    stationId = 1,
                    stationDesc = "Stop 1",
                    stationCode = "S1",
                    stationAlias = "S1",
                    stationLatitude = 53.0,
                    stationLongitude = -6.0,
                ),
                DartStationsResponseData.DartStationData(
                    stationId = 2,
                    stationDesc = "Stop 2",
                    stationCode = "S2",
                    stationAlias = "S2",
                    stationLatitude = 54.0,
                    stationLongitude = -5.0,
                ),
            ),
        )
        every { stationsDao.fetchAll() } returns flowOf(emptyList)
        coEvery { stationsApi.fetchStations() } returns Result.success(lineResponse)

        val daoStops = lineResponse.toDao()
        coEvery { stationsDao.insert(daoStops) } just Runs

        val result = repository.fetchStations().first()

        assertTrue(result.isSuccess)
        verify(exactly = 1) { stationsDao.fetchAll() }
        coVerify(exactly = 1) { stationsApi.fetchStations() }
        coVerify(exactly = 1) { stationsDao.insert(stations = any()) }
    }

    @Test
    fun `fetchStops should handle remote fetch error gracefully`() = runTest {
        val exception = RuntimeException("Network error")
        val expectedStops = emptyList<LuasStopEntity>()
        every { stationsDao.fetchAll() } returns flowOf(emptyList())
        coEvery { stationsApi.fetchStations() } returns Result.failure(exception)

        val result = repository.fetchStations().first()

        assertTrue(result.isSuccess)
        assertEquals(expectedStops, result.getOrNull())
        verify(exactly = 1) { stationsDao.fetchAll() }
        coVerify(exactly = 1) { stationsApi.fetchStations() }
        coVerify(exactly = 0) { stationsDao.insert(stations = any()) }
    }

    @Test
    fun `fetchAllStops should return all stops from database`() = runTest {
        val dbStops = listOf(
            DartStation(
                Id = 1,
                Name = "Stop 1",
                Code = "S1",
                Alias = "S1",
                Latitude = 53.0,
                Longitude = -6.0,
            ),
            DartStation(
                Id = 2,
                Name = "Stop 2",
                Code = "S2",
                Alias = "S2",
                Latitude = 54.0,
                Longitude = -5.0,
            ),
        )
        val expectedStops = dbStops.map { it.toEntity() }

        every { stationsDao.fetchAll() } returns flowOf(dbStops)

        val result = repository.fetchStations().first()

        assertTrue(result.isSuccess)
        assertEquals(expectedStops, result.getOrNull())
        verify(exactly = 1) { stationsDao.fetchAll() }
    }

    @Test
    fun `fetchForecast should return forecast for stop`() = runTest {
        val stopAbv = "S1"
        val forecastResponse = DartStationForecastResponseData(
            stationData = listOf(
                DartStationForecastData(
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
                    direction = DartDirectionData.NORTHBOUND,
                    trainType = "DART",
                    locationType = DartLocationTypeData.ORIGIN,
                ),
                DartStationForecastData(
                    serverTime = "2025-06-26T21:35:15.323",
                    trainCode = "E950",
                    stationFullName = "Greystones",
                    stationCode = "GSTNS",
                    queryTime = "21:35:15",
                    trainDate = "26 Jun 2025",
                    origin = "Greystones",
                    destination = "Howth",
                    originTime = "22:20",
                    destinationTime = "23:45",
                    status = "No Information",
                    lastLocation = "",
                    dueIn = 45,
                    late = 0,
                    expArrival = "00:00",
                    expDepart = "22:20",
                    schArrival = "00:00",
                    schDepart = "22:20",
                    direction = DartDirectionData.NORTHBOUND,
                    trainType = "DART",
                    locationType = DartLocationTypeData.ORIGIN,
                ),
            ),
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
