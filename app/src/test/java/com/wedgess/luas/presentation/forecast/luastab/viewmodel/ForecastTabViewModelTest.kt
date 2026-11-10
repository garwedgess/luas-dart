package com.wedgess.luas.presentation.forecast.luastab.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.wedgess.luas.domain.alarm.AlarmCoordinator
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmCapabilities
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmRequest
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.model.StationPreferences
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.repository.StationPreferencesRepository
import com.wedgess.luas.presentation.forecast.luastab.LuasForecastTabContract
import com.wedgess.luas.presentation.forecast.luastab.model.LuasForecastTabDialogState
import com.wedgess.luas.presentation.model.UiResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@ExperimentalCoroutinesApi
class ForecastTabViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val luasLine = LuasLineEntity.GREEN

    @MockK
    private lateinit var luasRepository: LuasRepository

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository

    @MockK
    private lateinit var alarmCoordinator: AlarmCoordinator

    @MockK
    private lateinit var stationPreferencesRepository: StationPreferencesRepository

    private val preferencesFlow = MutableStateFlow(StationPreferences())

    private lateinit var viewModel: LuasForecastTabViewModel

    private val mockStops = listOf(
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

    private val now = Instant.parse("2026-11-09T22:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private val mockForecast = Departures(
        stationCode = "STA",
        stationName = "St. Stephens Green",
        fetchedAt = now,
        message = "Trams operating normally",
        items = listOf(
            Departure(
                transport = TransportType.LUAS,
                direction = DepartureDirection.INBOUND,
                destination = "Broombridge",
                dueMins = 2,
                expectedAt = now.plusSeconds(120)
            )
        )
    )

    private val stopsFlow = MutableStateFlow(Result.success(mockStops))
    private var forecastResult: Result<Departures> = Result.success(mockForecast)
    private var cachedForecast: Departures? = null
    private val selectedStationFlow = MutableStateFlow("")
    private val activeAlarmsFlow = MutableStateFlow<List<Alarm>>(emptyList())
    private var capabilities = AlarmCapabilities(exactAlarms = true, notifications = true)

    private fun showAlarm(minutes: Int) = LuasForecastTabContract.Event.OnShowNotificationsDialog(
        "STA",
        minutes,
        "Broombridge",
        DepartureDirection.INBOUND
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { luasRepository.fetchStops(any()) } returns stopsFlow
        coEvery { luasRepository.fetchForecast(any()) } answers { forecastResult }
        coEvery { luasRepository.cachedForecast(any()) } answers { cachedForecast }
        every { preferencesRepository.fetchSelectedGreenLineStation() } returns selectedStationFlow
        every { preferencesRepository.fetchSelectedRedLineStation() } returns selectedStationFlow
        every { stationPreferencesRepository.observe(any()) } returns preferencesFlow
        coEvery { stationPreferencesRepository.markUsed(any(), any()) } returns Result.success(Unit)
        coEvery { stationPreferencesRepository.setFavourite(any(), any(), any()) } returns Result.success(Unit)
        every { alarmCoordinator.observeActive() } returns activeAlarmsFlow
        every { alarmCoordinator.capabilities } answers { capabilities }
        every { alarmCoordinator.openExactAlarmSettings() } returns Unit
        coEvery { alarmCoordinator.cancel(any()) } returns Result.success(true)
        coEvery { alarmCoordinator.create(any()) } answers { Result.success(mockk(relaxed = true)) }
        coEvery { preferencesRepository.updateSelectedGreenLineStation(any()) } returns Result.success(Unit)

        viewModel = LuasForecastTabViewModel(
            luasLine = luasLine,
            luasRepository = luasRepository,
            preferencesRepository = preferencesRepository,
            alarmCoordinator = alarmCoordinator,
            stationPreferencesRepository = stationPreferencesRepository,
            clock = clock
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should select first stop when no selected station is available`() = runTest {
        // Given
        selectedStationFlow.value = ""

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            assertEquals(mockStops.first().name, state.selectedStop.text)
        }
    }

    @Test
    fun `should find and select matching stop from selected station abbreviation`() = runTest {
        // Given
        selectedStationFlow.value = "Harcourt"

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            assertEquals("Harcourt", state.selectedStop.text)
        }
    }

    @Test
    fun `should fall back to the first stop when a stored selection has disappeared`() = runTest {
        // Given
        selectedStationFlow.value = "NON_EXISTENT"

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            // A removed saved stop must not cause a forecast request with an empty code.
            assertEquals(mockStops.first().name, state.selectedStop.text)
        }
    }

    @Test
    fun `should handle empty stops list gracefully`() = runTest {
        // Given
        stopsFlow.value = Result.success(emptyList())

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Empty)
            assertEquals("No stops found", (result as UiResult.Empty).message)
        }
    }

    @Test
    fun `should handle error when fetching stops`() = runTest {
        // Given
        val errorMsg = "Network error"
        stopsFlow.value = Result.failure(Exception(errorMsg))

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Error)
            assertEquals(errorMsg, (result as UiResult.Error).message)
        }
    }

    @Test
    fun `should pass error from forecast to UI state`() = runTest {
        // Given
        val errorMsg = "Forecast error"
        forecastResult = Result.failure(Exception(errorMsg))

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Error)
            assertEquals(errorMsg, (result as UiResult.Error).message)
        }
    }

    @Test
    fun `should expose departures from the latest live fetch`() = runTest {
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            assertEquals(mockForecast, state.forecast)
            assertFalse(state.isStale)
        }
    }

    @Test
    fun `should show stored data as stale until the first live fetch completes`() = runTest {
        cachedForecast = mockForecast.copy(fetchedAt = now.minusSeconds(60))
        val live = mockForecast.copy(message = "live")
        val gate = CompletableDeferred<Unit>()
        coEvery { luasRepository.fetchForecast(any()) } coAnswers {
            gate.await()
            Result.success(live)
        }

        viewModel.uiResult.test {
            val restored = (awaitItem() as UiResult.Success).data
            assertTrue(restored.isStale)
            assertEquals(now.minusSeconds(60), restored.forecast.fetchedAt)

            gate.complete(Unit)
            val fresh = (awaitItem() as UiResult.Success).data
            assertFalse(fresh.isStale)
            assertEquals("live", fresh.forecast.message)
        }
    }

    @Test
    fun `should not offer an alarm from stored data before the first live fetch`() = runTest {
        cachedForecast = mockForecast
        coEvery { luasRepository.fetchForecast(any()) } coAnswers { awaitCancellation() }

        viewModel.uiResult.test {
            assertTrue((awaitItem() as UiResult.Success).data.isStale)
            viewModel.onEvent(
                showAlarm(5)
            )
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should keep last data marked stale when a refresh fails and drop departures already past`() = runTest {
        val pastDeparture = mockForecast.items.first().copy(destination = "Past", expectedAt = now.minusSeconds(30))
        forecastResult = Result.success(mockForecast.copy(items = mockForecast.items + pastDeparture))

        viewModel.uiResult.test {
            assertFalse((awaitItem() as UiResult.Success).data.isStale)

            forecastResult = Result.failure(Exception("offline"))
            viewModel.onEvent(LuasForecastTabContract.Event.OnRefresh)

            val stale = (awaitItem() as UiResult.Success).data
            assertTrue(stale.isStale)
            assertEquals(listOf("Broombridge"), stale.forecast.items.map { it.destination })
        }
    }

    @Test
    fun `should show an error once stale data is older than the maximum age`() = runTest {
        forecastResult = Result.success(mockForecast.copy(fetchedAt = now.minusSeconds(11 * 60L)))

        viewModel.uiResult.test {
            assertTrue(awaitItem() is UiResult.Success)

            forecastResult = Result.failure(Exception("offline"))
            viewModel.onEvent(LuasForecastTabContract.Event.OnRefresh)

            assertEquals("offline", (awaitItem() as UiResult.Error).message)
        }
    }

    @Test
    fun `should not offer an alarm from stale data`() = runTest {
        viewModel.uiResult.test {
            awaitItem()
            forecastResult = Result.failure(Exception("offline"))
            viewModel.onEvent(LuasForecastTabContract.Event.OnRefresh)
            assertTrue((awaitItem() as UiResult.Success).data.isStale)

            viewModel.onEvent(
                showAlarm(5)
            )
            expectNoEvents()
        }
        assertEquals(
            LuasForecastTabDialogState.None,
            viewModel.uiResult.value.let { (it as UiResult.Success).data.dialog }
        )
    }

    @Test
    fun `should update selected stop when OnStopSelected event is received`() = runTest {
        // Given
        val selectedStop = mockStops[1] // Harcourt

        // When
        viewModel.onEvent(LuasForecastTabContract.Event.OnStopSelected(selectedStop.name))

        // Then
        coVerify { preferencesRepository.updateSelectedGreenLineStation(selectedStop.name) }
    }

    @Test
    fun `should show travel updates dialog when OnShowTravelUpdatesDialog event is received`() = runTest {
        viewModel.uiResult.test {
            awaitItem()
            viewModel.onEvent(LuasForecastTabContract.Event.OnShowTravelUpdatesDialog)
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            assertEquals(LuasForecastTabDialogState.TravelUpdatesAlert, (result as UiResult.Success).data.dialog)
        }
    }

    @Test
    fun `should fetch again immediately when OnRefresh event is received`() = runTest {
        viewModel.uiResult.test {
            awaitItem()
            viewModel.onEvent(LuasForecastTabContract.Event.OnRefresh)
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 2) { luasRepository.fetchForecast("STA") }
    }

    @Test
    fun `should poll every 15 seconds while observed`() = runTest {
        viewModel.uiResult.test {
            awaitItem()
            advanceTimeBy(LuasForecastTabContract.REFRESH_INTERVAL_MILLIS + 1)
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 2) { luasRepository.fetchForecast("STA") }
    }

    @Test
    fun `should handle non-existent stop when OnStopSelected is called`() = runTest {
        // Given a stop that's not in the list
        val nonExistentStop = LuasStopEntity(
            id = UUID.randomUUID(),
            abbreviation = "NON",
            name = "Non-existent Stop",
            latitude = 0.0,
            longitude = 0.0,
            line = LuasLineEntity.GREEN,
            isParkAndRide = false,
            isCycleAndRide = false
        )

        // When
        viewModel.onEvent(LuasForecastTabContract.Event.OnStopSelected(nonExistentStop.name))

        // Then should still attempt to update the selected station
        coVerify { preferencesRepository.updateSelectedGreenLineStation(nonExistentStop.name) }
    }

    @Test
    fun `should handle update of selected station failure gracefully`() = runTest {
        // Given
        coEvery {
            preferencesRepository.updateSelectedGreenLineStation(any())
        } returns Result.failure(Exception("Database error"))

        // When - should not crash
        viewModel.onEvent(LuasForecastTabContract.Event.OnStopSelected(mockStops[1].name))

        // Then - test should complete without exceptions
    }

    @Test
    fun `should handle null error message from forecast exception`() = runTest {
        // Given
        forecastResult = Result.failure(Exception()) // No message

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Error)
            assertEquals("Failed to fetch forecast", (result as UiResult.Error).message)
        }
    }

    private fun alarm(id: String, station: String = "STA") = Alarm(
        id = id,
        requestCode = 1,
        kind = AlarmKind.SPECIFIC_DEPARTURE,
        state = AlarmState.ACTIVE,
        transport = TransportType.LUAS,
        stationCode = station,
        stationName = "Station $station",
        destination = "Broombridge",
        notifyMinutesBefore = 3,
        reminderAt = now,
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun `opening the alarm dialog shows it straight away without waiting for the next poll`() = runTest {
        viewModel.uiResult.test {
            awaitItem()
            viewModel.onEvent(
                showAlarm(5)
            )

            val state = (awaitItem() as UiResult.Success).data
            assertEquals(LuasForecastTabDialogState.Notification, state.dialog)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the alarm dialog records the live expected arrival and the station code`() = runTest {
        viewModel.uiResult.test {
            awaitItem()
            viewModel.onEvent(
                showAlarm(5)
            )

            val pending = (awaitItem() as UiResult.Success).data.luasNotificationState
            assertEquals("STA", pending.stationCode)
            assertEquals(DepartureDirection.INBOUND, pending.direction)
            // The fetch time plus the live minutes, not a countdown from whenever the dialog happened to open.
            assertEquals(mockForecast.fetchedAt.plusSeconds(300), pending.expectedAt)
            assertEquals(5, pending.notifyMinutesBefore)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirming creates a specific-departure alarm request through the coordinator`() = runTest {
        val request = slot<AlarmRequest>()
        coEvery { alarmCoordinator.create(capture(request)) } returns Result.success(mockk(relaxed = true))

        viewModel.uiResult.test {
            awaitItem()
            viewModel.onEvent(
                showAlarm(8)
            )
            awaitItem()
            viewModel.onEvent(LuasForecastTabContract.Event.OnStartNotification(minutes = 3))
            cancelAndIgnoreRemainingEvents()
        }

        with(request.captured) {
            assertEquals(AlarmKind.SPECIFIC_DEPARTURE, kind)
            assertEquals(TransportType.LUAS, transport)
            assertEquals("STA", stationCode)
            assertEquals("St. Stephens Green", stationName)
            assertEquals(DepartureDirection.INBOUND, direction)
            assertEquals("Broombridge", destination)
            assertEquals(3, notifyMinutesBefore)
            assertEquals(mockForecast.fetchedAt.plusSeconds(480), expectedAt)
            assertEquals(mockForecast.fetchedAt, observedAt)
            assertTrue(requestId.isNotBlank())
        }
    }

    @Test
    fun `confirming the same dialog twice sends the same request id so it stays one alarm`() = runTest {
        val requests = mutableListOf<AlarmRequest>()
        coEvery { alarmCoordinator.create(capture(requests)) } returns Result.success(mockk(relaxed = true))

        viewModel.uiResult.test {
            awaitItem()
            viewModel.onEvent(
                showAlarm(8)
            )
            awaitItem()
            viewModel.onEvent(LuasForecastTabContract.Event.OnStartNotification(minutes = 3))
            viewModel.onEvent(LuasForecastTabContract.Event.OnStartNotification(minutes = 3))
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, requests.map { it.requestId }.toSet().size)
    }

    @Test
    fun `opening the dialog reads what the device allows but never prompts`() = runTest {
        capabilities = AlarmCapabilities(exactAlarms = false, notifications = false)

        viewModel.uiResult.test {
            awaitItem()
            viewModel.onEvent(
                showAlarm(5)
            )

            val state = (awaitItem() as UiResult.Success).data
            assertEquals(capabilities, state.alarmCapabilities)
            assertEquals(LuasForecastTabDialogState.Notification, state.dialog)
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 0) { alarmCoordinator.openExactAlarmSettings() }
    }

    @Test
    fun `settings are only opened when the user asks`() = runTest {
        viewModel.onEvent(LuasForecastTabContract.Event.OnOpenExactAlarmSettings)

        verify(exactly = 1) { alarmCoordinator.openExactAlarmSettings() }
    }

    @Test
    fun `cancelling cancels that alarm by id`() = runTest {
        viewModel.onEvent(LuasForecastTabContract.Event.OnStopNotification("alarm-7"))

        coVerify { alarmCoordinator.cancel("alarm-7") }
    }

    @Test
    fun `active alarms of every station are shown and survive changing the viewed station`() = runTest {
        activeAlarmsFlow.value = listOf(alarm("a", "STA"), alarm("b", "HAR"))

        viewModel.uiResult.test {
            assertEquals(listOf("a", "b"), (awaitItem() as UiResult.Success).data.activeAlarms.map { it.id })

            viewModel.onEvent(LuasForecastTabContract.Event.OnStopSelected(mockStops[1].name))

            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { alarmCoordinator.cancel(any()) }
    }

    @Test
    fun `only Luas alarms appear in the Luas list`() = runTest {
        activeAlarmsFlow.value = listOf(alarm("luas"), alarm("dart").copy(transport = TransportType.DART))

        viewModel.uiResult.test {
            assertEquals(listOf("luas"), (awaitItem() as UiResult.Success).data.activeAlarms.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an alarm finishing removes it from the list without waiting for a poll`() = runTest {
        activeAlarmsFlow.value = listOf(alarm("a"))

        viewModel.uiResult.test {
            assertEquals(1, (awaitItem() as UiResult.Success).data.activeAlarms.size)
            activeAlarmsFlow.value = emptyList()
            assertEquals(0, (awaitItem() as UiResult.Success).data.activeAlarms.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `zero or negative minutes do not open the alarm dialog`() = runTest {
        viewModel.uiResult.test {
            awaitItem()
            viewModel.onEvent(
                showAlarm(0)
            )
            viewModel.onEvent(
                showAlarm(-5)
            )
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favouriting a station is stored for this transport only`() = runTest {
        viewModel.onEvent(LuasForecastTabContract.Event.OnToggleFavourite("Harcourt", true))

        coVerify { stationPreferencesRepository.setFavourite(TransportType.LUAS, any(), true) }
    }

    @Test
    fun `picking a station records it as recently used for this transport`() = runTest {
        viewModel.onEvent(LuasForecastTabContract.Event.OnStopSelected("Harcourt"))

        coVerify { stationPreferencesRepository.markUsed(TransportType.LUAS, any()) }
    }

    @Test
    fun `stored favourites and recents reach the state`() = runTest {
        preferencesFlow.value = StationPreferences(setOf("A"), listOf("B"))

        viewModel.uiResult.test {
            val state = (awaitItem() as UiResult.Success).data
            assertEquals(setOf("A"), state.stationPreferences.favourites)
            assertEquals(listOf("B"), state.stationPreferences.recents)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a network failure with nothing to show is reported as offline, not as an error`() = runTest {
        forecastResult = Result.failure(Exception("Network error occurred", java.io.IOException("no route")))

        viewModel.uiResult.test {
            assertEquals("Network error occurred", (awaitItem() as UiResult.Offline).message)
        }
    }

    @Test
    fun `station switch hides old rows until its own response and rejects an old station tap`() = runTest {
        val next = CompletableDeferred<Result<Departures>>()
        coEvery { luasRepository.fetchForecast("HAR") } coAnswers { next.await() }
        val rendered = mutableListOf<UiResult<LuasForecastTabContract.UiState>>()
        backgroundScope.launch(testDispatcher) { viewModel.uiResult.collect { rendered.add(it) } }
        assertEquals("STA", (viewModel.uiResult.value as UiResult.Success).data.forecastStationCode)

        selectedStationFlow.value = "Harcourt"
        runCurrent()
        assertTrue(viewModel.uiResult.value is UiResult.Loading)
        assertFalse(
            rendered.filterIsInstance<UiResult.Success<LuasForecastTabContract.UiState>>().any {
                it.data.selectedStop.text == "Harcourt" && it.data.forecast.stationCode == "STA"
            }
        )
        viewModel.onEvent(showAlarm(5))
        viewModel.onEvent(LuasForecastTabContract.Event.OnStartNotification(1))
        coVerify(exactly = 0) { alarmCoordinator.create(any()) }

        next.complete(Result.success(mockForecast.copy(stationCode = "HAR", stationName = "Harcourt")))
        runCurrent()
        assertEquals("HAR", (viewModel.uiResult.value as UiResult.Success).data.forecastStationCode)
        viewModel.onEvent(showAlarm(5))
        assertEquals(LuasForecastTabDialogState.None, (viewModel.uiResult.value as UiResult.Success).data.dialog)
        viewModel.onEvent(showAlarm(5).copy(stationCode = "HAR"))
        assertEquals("HAR", (viewModel.uiResult.value as UiResult.Success).data.luasNotificationState.stationCode)
    }

    @Test
    fun `new station cache is shown as stale while its live fetch is pending`() = runTest {
        coEvery { luasRepository.cachedForecast("HAR") } returns mockForecast.copy(
            stationCode = "HAR",
            stationName = "Harcourt",
            fetchedAt = now.minusSeconds(60)
        )
        coEvery { luasRepository.fetchForecast("HAR") } coAnswers { awaitCancellation() }
        backgroundScope.launch(testDispatcher) { viewModel.uiResult.collect {} }
        selectedStationFlow.value = "Harcourt"
        runCurrent()
        val state = (viewModel.uiResult.value as UiResult.Success).data
        assertTrue(state.isStale)
        assertEquals("HAR", state.forecastStationCode)
        assertEquals("HAR", state.forecast.stationCode)
    }

    @Test
    fun `station selection immediately discards an open alarm draft before preferences finish saving`() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.uiResult.collect {} }
        viewModel.onEvent(showAlarm(5))
        assertEquals(
            LuasForecastTabDialogState.Notification,
            (viewModel.uiResult.value as UiResult.Success).data.dialog
        )
        viewModel.onEvent(LuasForecastTabContract.Event.OnStopSelected("Harcourt"))
        viewModel.onEvent(LuasForecastTabContract.Event.OnStartNotification(1))
        coVerify(exactly = 0) { alarmCoordinator.create(any()) }
    }
}
