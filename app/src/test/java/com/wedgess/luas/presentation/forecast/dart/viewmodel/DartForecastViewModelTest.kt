package com.wedgess.luas.presentation.forecast.dart.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.wedgess.luas.domain.alarm.AlarmCoordinator
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmCapabilities
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmRequest
import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.StationPreferences
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.repository.StationPreferencesRepository
import com.wedgess.luas.presentation.forecast.dart.DartForecastTabContract
import com.wedgess.luas.presentation.model.UiResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.time.LocalDate
import java.time.ZoneOffset

@ExperimentalCoroutinesApi
class DartForecastViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @MockK
    private lateinit var dartRepository: DartRepository

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository

    @MockK
    private lateinit var alarmCoordinator: AlarmCoordinator

    @MockK
    private lateinit var stationPreferencesRepository: StationPreferencesRepository

    private val activeAlarms = MutableStateFlow<List<Alarm>>(emptyList())

    private val preferencesFlow = MutableStateFlow(StationPreferences())

    private lateinit var viewModel: DartForecastViewModel

    private val now = Instant.parse("2026-11-09T22:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private val mockStations = listOf(
        DartStationEntity(
            id = 1,
            name = "Dublin Connolly",
            alias = "Connolly",
            latitude = 53.3508,
            longitude = -6.2518,
            code = "CNLLY"
        ),
        DartStationEntity(
            id = 2,
            name = "Dublin Pearse",
            alias = "Pearse",
            latitude = 53.3437,
            longitude = -6.2494,
            code = "PERSE"
        )
    )

    private fun departure(
        tripCode: String,
        direction: DepartureDirection,
        dueMins: Int = 5,
        lateMins: Int = 0
    ) = Departure(
        transport = TransportType.DART,
        direction = direction,
        destination = "Bray",
        dueMins = dueMins,
        expectedAt = now.plusSeconds(dueMins * 60L),
        scheduledAt = now.plusSeconds((dueMins - lateMins) * 60L),
        lateMins = lateMins,
        tripCode = tripCode,
        operatingDate = LocalDate.of(2026, 11, 9),
        status = "On Time",
        lastLocation = "Howth Junction"
    )

    private fun departures(vararg items: Departure, fetchedAt: Instant = now) = Departures(
        stationCode = "CNLLY",
        stationName = "Dublin Connolly",
        fetchedAt = fetchedAt,
        message = "",
        items = items.toList()
    )

    private val southbound = departure("D123", DepartureDirection.SOUTHBOUND)
    private val northbound = departure("D456", DepartureDirection.NORTHBOUND, dueMins = 8)

    private val stationsFlow = MutableStateFlow(Result.success(mockStations))
    private val selectedStationFlow = MutableStateFlow("")
    private var forecastResult: Result<Departures> = Result.success(departures(southbound, northbound))
    private var cachedForecast: Departures? = null

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { dartRepository.fetchStations() } returns stationsFlow
        coEvery { dartRepository.fetchForecast(any()) } answers { forecastResult }
        coEvery { dartRepository.cachedForecast(any()) } answers { cachedForecast }
        every { preferencesRepository.fetchSelectedDartStation() } returns selectedStationFlow
        coEvery { preferencesRepository.updateSelectedDartStation(any()) } returns Result.success(Unit)

        every { stationPreferencesRepository.observe(any()) } returns preferencesFlow
        coEvery { stationPreferencesRepository.markUsed(any(), any()) } returns Result.success(Unit)
        coEvery { stationPreferencesRepository.setFavourite(any(), any(), any()) } returns Result.success(Unit)
        every { alarmCoordinator.observeActive() } returns activeAlarms
        every { alarmCoordinator.capabilities } returns AlarmCapabilities(exactAlarms = true, notifications = true)
        every { alarmCoordinator.openExactAlarmSettings() } returns Unit
        coEvery { alarmCoordinator.cancel(any()) } returns Result.success(true)
        coEvery { alarmCoordinator.create(any()) } returns Result.success(mockk(relaxed = true))

        viewModel = DartForecastViewModel(
            dartRepository = dartRepository,
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

    private suspend fun app.cash.turbine.ReceiveTurbine<UiResult<DartForecastTabContract.UiState>>.awaitState() =
        (awaitItem() as UiResult.Success).data

    @Test
    fun `should select first station when no selected station is available`() = runTest {
        selectedStationFlow.value = ""

        viewModel.uiResult.test {
            assertEquals(mockStations.first().name, awaitState().selectedStation.text)
        }
        coVerify { dartRepository.fetchForecast("CNLLY") }
    }

    @Test
    fun `should find and select matching station from selected station name`() = runTest {
        selectedStationFlow.value = "Dublin Pearse"

        viewModel.uiResult.test {
            assertEquals("Dublin Pearse", awaitState().selectedStation.text)
        }
        coVerify { dartRepository.fetchForecast("PERSE") }
    }

    @Test
    fun `should use initial station when selected station not found in stations list`() = runTest {
        selectedStationFlow.value = "NON_EXISTENT"

        viewModel.uiResult.test {
            assertEquals("", awaitState().selectedStation.text)
        }
    }

    @Test
    fun `should handle empty stations list gracefully`() = runTest {
        stationsFlow.value = Result.success(emptyList())

        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Empty)
            assertEquals("No stops found", (result as UiResult.Empty).message)
        }
    }

    @Test
    fun `should handle error when fetching stations`() = runTest {
        stationsFlow.value = Result.failure(Exception("Network error"))

        viewModel.uiResult.test {
            assertEquals("Network error", (awaitItem() as UiResult.Error).message)
        }
    }

    @Test
    fun `should handle null error message from stations exception`() = runTest {
        stationsFlow.value = Result.failure(Exception())

        viewModel.uiResult.test {
            assertEquals("Failed to fetch stations", (awaitItem() as UiResult.Error).message)
        }
    }

    @Test
    fun `should pass error from forecast to UI state when there is nothing to show`() = runTest {
        forecastResult = Result.failure(Exception("Forecast error"))

        viewModel.uiResult.test {
            assertEquals("Forecast error", (awaitItem() as UiResult.Error).message)
        }
    }

    @Test
    fun `should handle null error message from forecast exception`() = runTest {
        forecastResult = Result.failure(Exception())

        viewModel.uiResult.test {
            assertEquals("Failed to fetch forecast", (awaitItem() as UiResult.Error).message)
        }
    }

    @Test
    fun `should expose the fetch time and a live state after a successful fetch`() = runTest {
        viewModel.uiResult.test {
            val state = awaitState()
            assertFalse(state.isStale)
            assertEquals(now, state.fetchedAt)
        }
    }

    @Test
    fun `should show stored trains as stale with their original fetch time until the first live fetch`() = runTest {
        val storedAt = now.minusSeconds(90)
        cachedForecast = departures(southbound, fetchedAt = storedAt)
        val gate = CompletableDeferred<Unit>()
        coEvery { dartRepository.fetchForecast(any()) } coAnswers {
            gate.await()
            forecastResult
        }

        viewModel.uiResult.test {
            val restored = awaitState()
            assertTrue(restored.isStale)
            assertEquals(storedAt, restored.fetchedAt)

            gate.complete(Unit)
            val fresh = awaitState()
            assertFalse(fresh.isStale)
            assertEquals(now, fresh.fetchedAt)
        }
    }

    @Test
    fun `should keep last data marked stale when a refresh fails`() = runTest {
        viewModel.uiResult.test {
            assertFalse(awaitState().isStale)

            forecastResult = Result.failure(Exception("offline"))
            viewModel.onEvent(DartForecastTabContract.Event.OnRefresh)

            val stale = awaitState()
            assertTrue(stale.isStale)
            assertEquals(2, stale.sectionedListState.sections.sumOf { it.items.size })
        }
    }

    @Test
    fun `should drop trains already past and show an error once stale data is too old`() = runTest {
        forecastResult = Result.success(departures(southbound, fetchedAt = now.minusSeconds(11 * 60L)))

        viewModel.uiResult.test {
            assertTrue(awaitItem() is UiResult.Success)

            forecastResult = Result.failure(Exception("offline"))
            viewModel.onEvent(DartForecastTabContract.Event.OnRefresh)

            assertEquals("offline", (awaitItem() as UiResult.Error).message)
        }
    }

    @Test
    fun `should update selected station when OnStationSelected event is received`() = runTest {
        viewModel.onEvent(DartForecastTabContract.Event.OnStationSelected(mockStations[1].name))

        coVerify { preferencesRepository.updateSelectedDartStation("Dublin Pearse") }
    }

    @Test
    fun `should handle update of selected station failure gracefully`() = runTest {
        coEvery { preferencesRepository.updateSelectedDartStation(any()) } returns
            Result.failure(Exception("Database error"))

        viewModel.onEvent(DartForecastTabContract.Event.OnStationSelected(mockStations[1].name))
    }

    @Test
    fun `should fetch again immediately when OnRefresh event is received`() = runTest {
        viewModel.uiResult.test {
            awaitItem()
            viewModel.onEvent(DartForecastTabContract.Event.OnRefresh)
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 2) { dartRepository.fetchForecast("CNLLY") }
    }

    @Test
    fun `should poll every 40 seconds while observed`() = runTest {
        viewModel.uiResult.test {
            awaitItem()
            advanceTimeBy(DartForecastTabContract.REFRESH_INTERVAL_MILLIS + 1)
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 2) { dartRepository.fetchForecast("CNLLY") }
    }

    @Test
    fun `should create sections for all directions even when no forecasts exist`() = runTest {
        forecastResult = Result.success(departures())

        viewModel.uiResult.test {
            val titles = awaitState().sectionedListState.sections.map { it.title }
            assertEquals(listOf("Northbound", "Southbound"), titles)
        }
    }

    @Test
    fun `should group forecasts by direction correctly`() = runTest {
        viewModel.uiResult.test {
            val sections = awaitState().sectionedListState.sections
            val northbound = sections.first { it.title == DepartureDirection.NORTHBOUND.key }
            val southbound = sections.first { it.title == DepartureDirection.SOUTHBOUND.key }

            assertEquals(1, northbound.items.size)
            assertEquals(1, southbound.items.size)
            assertEquals("1 train", northbound.subTitle)
            assertEquals("1 train", southbound.subTitle)
        }
    }

    @Test
    fun `should show correct subtitle for multiple trains`() = runTest {
        forecastResult = Result.success(
            departures(
                southbound,
                departure("D789", DepartureDirection.SOUTHBOUND, dueMins = 12),
                departure("D999", DepartureDirection.SOUTHBOUND, dueMins = 15)
            )
        )

        viewModel.uiResult.test {
            val southboundSection = awaitState().sectionedListState.sections
                .first { it.title == DepartureDirection.SOUTHBOUND.key }
            assertEquals("3 trains", southboundSection.subTitle)
        }
    }

    @Test
    fun `should show 'No trains' subtitle when direction has no forecasts`() = runTest {
        forecastResult = Result.success(departures(southbound))

        viewModel.uiResult.test {
            val northboundSection = awaitState().sectionedListState.sections
                .first { it.title == DepartureDirection.NORTHBOUND.key }
            assertEquals("No trains", northboundSection.subTitle)
        }
    }

    @Test
    fun `should ignore trains with an unknown direction`() = runTest {
        forecastResult = Result.success(departures(departure("G1", DepartureDirection.UNKNOWN)))

        viewModel.uiResult.test {
            assertEquals(0, awaitState().sectionedListState.sections.sumOf { it.items.size })
        }
    }

    @Test
    fun `should toggle section expansion state when OnToggleSection event is received`() = runTest {
        val sectionId = DepartureDirection.NORTHBOUND.id

        viewModel.onEvent(DartForecastTabContract.Event.OnToggleSection(sectionId))
        viewModel.uiResult.test {
            // Sections default to expanded, so the first toggle collapses.
            assertEquals(false, awaitState().sectionedListState.expandedSections[sectionId])
        }

        viewModel.onEvent(DartForecastTabContract.Event.OnToggleSection(sectionId))
        viewModel.uiResult.test {
            assertEquals(true, awaitState().sectionedListState.expandedSections[sectionId])
        }
    }

    @Test
    fun `should map departure data correctly to section row data`() = runTest {
        val late = departure("D123", DepartureDirection.SOUTHBOUND, dueMins = 5, lateMins = 2)
        forecastResult = Result.success(departures(late))

        viewModel.uiResult.test {
            val row = awaitState().sectionedListState.sections
                .first { it.title == DepartureDirection.SOUTHBOUND.key }.items.first().content

            assertEquals("D123", row.trainCode)
            assertEquals(5, row.dueIn)
            assertEquals("Bray", row.destination)
            assertEquals("On Time", row.status)
            assertEquals("Howth Junction", row.lastLocation)
            assertEquals(2, row.late)
            assertEquals(DepartureDirection.SOUTHBOUND, row.direction)
            // 22:00Z is 22:00 in Dublin in November (GMT); expected is 5 min later and scheduled 2 min earlier.
            assertEquals("22:05", row.expectedAt)
            assertEquals("22:03", row.scheduledAt)
        }
    }

    private val show = DartForecastTabContract.Event.OnShowAlarmDialog(
        tripCode = "D123",
        operatingDate = LocalDate.of(2026, 11, 9),
        dueInMins = 8,
        destination = "Bray",
        direction = DepartureDirection.SOUTHBOUND
    )

    @Test
    fun `starting an alarm sends the train code, operating date and live expected time`() = runTest {
        val request = slot<AlarmRequest>()
        coEvery { alarmCoordinator.create(capture(request)) } returns Result.success(mockk(relaxed = true))

        viewModel.uiResult.test {
            awaitItem()
            viewModel.onEvent(show)
            val draft = awaitState().alarmDraft!!
            assertEquals("D123", draft.tripCode)
            assertEquals(now.plusSeconds(480), draft.expectedAt)
            viewModel.onEvent(DartForecastTabContract.Event.OnStartAlarm(3))
            cancelAndIgnoreRemainingEvents()
        }

        with(request.captured) {
            assertEquals(AlarmKind.SPECIFIC_DEPARTURE, kind)
            assertEquals(TransportType.DART, transport)
            assertEquals("CNLLY", stationCode)
            assertEquals("D123", tripCode)
            assertEquals(LocalDate.of(2026, 11, 9), operatingDate)
            assertEquals(3, notifyMinutesBefore)
            assertEquals(now.plusSeconds(480), expectedAt)
        }
    }

    @Test
    fun `no alarm dialog from stale data or a train that is already due`() = runTest {
        viewModel.uiResult.test {
            awaitItem()
            viewModel.onEvent(show.copy(dueInMins = 0))
            forecastResult = Result.failure(Exception("offline"))
            viewModel.onEvent(DartForecastTabContract.Event.OnRefresh)
            assertTrue(awaitState().isStale)
            viewModel.onEvent(show)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `only DART alarms are listed and cancelling goes through the coordinator`() = runTest {
        val dartAlarm = mockk<Alarm>(relaxed = true) {
            every { transport } returns TransportType.DART
            every { id } returns "d"
        }
        val luasAlarm = mockk<Alarm>(relaxed = true) { every { transport } returns TransportType.LUAS }
        activeAlarms.value = listOf(dartAlarm, luasAlarm)

        viewModel.uiResult.test {
            assertEquals(listOf("d"), awaitState().activeAlarms.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.onEvent(DartForecastTabContract.Event.OnCancelAlarm("d"))
        coVerify { alarmCoordinator.cancel("d") }
    }

    @Test
    fun `favouriting a station is stored for this transport only`() = runTest {
        viewModel.onEvent(DartForecastTabContract.Event.OnToggleFavourite("Dublin Pearse", true))

        coVerify { stationPreferencesRepository.setFavourite(TransportType.DART, any(), true) }
    }

    @Test
    fun `picking a station records it as recently used for this transport`() = runTest {
        viewModel.onEvent(DartForecastTabContract.Event.OnStationSelected("Dublin Pearse"))

        coVerify { stationPreferencesRepository.markUsed(TransportType.DART, any()) }
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
}
