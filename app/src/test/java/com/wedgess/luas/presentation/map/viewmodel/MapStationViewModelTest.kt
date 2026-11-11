package com.wedgess.luas.presentation.map.viewmodel

import com.wedgess.luas.domain.NetworkMonitor
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.usecase.FetchSelectedTransportTypeUseCase
import com.wedgess.luas.presentation.model.UiResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MapStationViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val luas = mockk<LuasRepository>()
    private val dart = mockk<DartRepository>()
    private val transport = MutableStateFlow(TransportType.LUAS)
    private val online = MutableStateFlow(true)
    private val networkMonitor = object : NetworkMonitor {
        override val isOnline = online
    }
    private val transportUseCase = mockk<FetchSelectedTransportTypeUseCase>()
    private val clock = mockk<Clock>()
    private val now = Instant.parse("2026-11-10T09:00:00Z")
    private val stop = LocationEntity.Luas("Harcourt", 53.33, -6.26, LuasLineEntity.GREEN, "HAR")
    private val station = LocationEntity.Dart("Bray", 53.20, -6.10, "BRAY")
    private val forecast = Departures(
        "HAR",
        "Harcourt",
        now,
        "",
        listOf(
            Departure(TransportType.LUAS, DepartureDirection.OUTBOUND, "Brides Glen", 8, now.plusSeconds(480))
        )
    )
    private lateinit var viewModel: MapStationViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { clock.instant() } returns now
        every { transportUseCase() } returns transport
        coEvery { luas.cachedForecast(any()) } returns null
        coEvery { dart.cachedForecast(any()) } returns null
        coEvery { luas.fetchForecast(any()) } returns Result.success(forecast)
        coEvery { dart.fetchForecast(any()) } returns Result.success(forecast.copy(stationCode = "BRAY"))
        viewModel = MapStationViewModel(luas, dart, transportUseCase, clock, networkMonitor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no selection does not fetch and station uses its API code`() = runTest {
        backgroundScope.launch(dispatcher) { viewModel.stationState.collect {} }
        assertNull(viewModel.stationState.value)
        coVerify(exactly = 0) { luas.fetchForecast(any()) }
        viewModel.select(stop)
        coVerify { luas.fetchForecast("HAR") }
        assertFalse(requireNotNull(viewModel.stationState.value).isStale)
    }

    @Test
    fun `DART selection uses DART provider and transport switch closes the sheet`() = runTest {
        backgroundScope.launch(dispatcher) { viewModel.stationState.collect {} }
        transport.value = TransportType.DART
        viewModel.select(station)
        coVerify { dart.fetchForecast("BRAY") }
        coVerify(exactly = 0) { luas.fetchForecast(any()) }
        transport.value = TransportType.LUAS
        assertNull(viewModel.stationState.value)
        advanceTimeBy(80_000)
        coVerify(exactly = 1) { dart.fetchForecast(any()) }
    }

    @Test
    fun `stored forecast is stale and failed refresh keeps only upcoming predictions`() = runTest {
        coEvery { luas.cachedForecast("HAR") } returns forecast.copy(
            items = forecast.items + forecast.items.first().copy(expectedAt = now.minusSeconds(60))
        )
        coEvery { luas.fetchForecast("HAR") } returns Result.failure(IOException())
        backgroundScope.launch(dispatcher) { viewModel.stationState.collect {} }
        viewModel.select(stop)
        val state = requireNotNull(viewModel.stationState.value)
        assertTrue(state.isStale)
        assertEquals(1, (state.result as UiResult.Success).data.items.size)
    }

    @Test
    fun `expired cache is hidden after a failed fetch`() = runTest {
        coEvery { luas.cachedForecast("HAR") } returns forecast.copy(fetchedAt = now.minusSeconds(601))
        coEvery { luas.fetchForecast("HAR") } returns Result.failure(IOException())
        backgroundScope.launch(dispatcher) { viewModel.stationState.collect {} }
        viewModel.select(stop)
        assertTrue(viewModel.stationState.value?.result is UiResult.Offline)
    }

    @Test
    fun `server failure is an error and retry recovers`() = runTest {
        coEvery { luas.fetchForecast("HAR") } returns Result.failure(IllegalStateException())
        backgroundScope.launch(dispatcher) { viewModel.stationState.collect {} }
        viewModel.select(stop)
        assertTrue(viewModel.stationState.value?.result is UiResult.Error)
        coEvery { luas.fetchForecast("HAR") } returns Result.success(forecast)
        viewModel.retry()
        assertTrue(viewModel.stationState.value?.result is UiResult.Success)
        assertFalse(requireNotNull(viewModel.stationState.value).isStale)
    }

    @Test
    fun `switching selection cancels an in-flight fetch and dismiss stops polling`() = runTest {
        var cancelled = false
        coEvery { luas.fetchForecast("HAR") } coAnswers {
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        backgroundScope.launch(dispatcher) { viewModel.stationState.collect {} }
        viewModel.select(stop)
        viewModel.select(stop.copy(name = "Cabra", stationCode = "CAB"))
        assertTrue(cancelled)
        assertEquals("CAB", viewModel.stationState.value?.station?.stationCode)
        viewModel.dismiss()
        advanceTimeBy(60_000)
        runCurrent()
        assertNull(viewModel.stationState.value)
        coVerify(exactly = 1) { luas.fetchForecast("CAB") }
    }

    @Test
    fun `an unidentifiable station does not trigger an API request`() = runTest {
        backgroundScope.launch(dispatcher) { viewModel.stationState.collect {} }
        viewModel.select(stop.copy(stationCode = ""))
        assertNull(viewModel.stationState.value)
        coVerify(exactly = 0) { luas.fetchForecast(any()) }
    }

    @Test
    fun `map forecasts also pause offline and refresh on reconnection`() = runTest {
        backgroundScope.launch(dispatcher) { viewModel.stationState.collect {} }
        viewModel.select(stop)
        coVerify(exactly = 1) { luas.fetchForecast("HAR") }
        online.value = false
        runCurrent()
        assertTrue(requireNotNull(viewModel.stationState.value).isOffline)
        assertTrue(requireNotNull(viewModel.stationState.value).isStale)
        viewModel.retry()
        advanceTimeBy(120_000)
        runCurrent()
        coVerify(exactly = 1) { luas.fetchForecast("HAR") }
        online.value = true
        runCurrent()
        coVerify(exactly = 2) { luas.fetchForecast("HAR") }
        assertFalse(requireNotNull(viewModel.stationState.value).isOffline)
        assertFalse(requireNotNull(viewModel.stationState.value).isStale)
    }
}
