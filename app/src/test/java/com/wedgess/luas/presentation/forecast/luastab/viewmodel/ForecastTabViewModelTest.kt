package com.wedgess.luas.presentation.forecast.luastab.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.CanScheduleExactAlarmsUseCase
import com.wedgess.luas.domain.usecase.CancelAlarmUseCase
import com.wedgess.luas.domain.usecase.FetchIsAlarmRunningUseCase
import com.wedgess.luas.domain.usecase.RequestExactAlarmPermissionUseCase
import com.wedgess.luas.domain.usecase.ScheduleAlarmUseCase
import com.wedgess.luas.presentation.forecast.luastab.LuasForecastTabContract
import com.wedgess.luas.presentation.forecast.luastab.model.LuasForecastTabDialogState
import com.wedgess.luas.presentation.model.UiResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
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
    private lateinit var canScheduleExactAlarmsUseCase: CanScheduleExactAlarmsUseCase

    @MockK
    private lateinit var cancelAlarmUseCase: CancelAlarmUseCase

    @MockK
    private lateinit var scheduleAlarmUseCase: ScheduleAlarmUseCase

    @MockK
    private lateinit var isAlarmRunningUseCase: FetchIsAlarmRunningUseCase

    @MockK
    private lateinit var requestExactAlarmPermissionUseCase: RequestExactAlarmPermissionUseCase

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
    private val selectedStationFlow = MutableStateFlow("")
    private val isAlarmRunningFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { luasRepository.fetchStops(any()) } returns stopsFlow
        coEvery { luasRepository.fetchForecast(any()) } answers { forecastResult }
        every { preferencesRepository.fetchSelectedGreenLineStation() } returns selectedStationFlow
        every { preferencesRepository.fetchSelectedRedLineStation() } returns selectedStationFlow
        every { isAlarmRunningUseCase() } returns isAlarmRunningFlow
        coEvery { preferencesRepository.updateSelectedGreenLineStation(any()) } returns Result.success(Unit)
        every { canScheduleExactAlarmsUseCase() } returns false
        every { cancelAlarmUseCase() } returns Unit
        coEvery { scheduleAlarmUseCase(any(), any()) } returns Result.success(0L)

        viewModel = LuasForecastTabViewModel(
            luasLine = luasLine,
            luasRepository = luasRepository,
            preferencesRepository = preferencesRepository,
            clock = clock,
            canScheduleExactAlarmsUseCase = canScheduleExactAlarmsUseCase,
            cancelAlarmUseCase = cancelAlarmUseCase,
            scheduleAlarmUseCase = scheduleAlarmUseCase,
            isAlarmRunningUseCase = isAlarmRunningUseCase,
            requestExactAlarmPermissionUseCase = requestExactAlarmPermissionUseCase
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
    fun `should use initial stop when selected stop not found in stops list`() = runTest {
        // Given
        selectedStationFlow.value = "NON_EXISTENT"

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            // Should default to StopEntity.initial()
            assertEquals("", state.selectedStop.text)
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
        every { canScheduleExactAlarmsUseCase() } returns true

        viewModel.uiResult.test {
            awaitItem()
            forecastResult = Result.failure(Exception("offline"))
            viewModel.onEvent(LuasForecastTabContract.Event.OnRefresh)
            assertTrue((awaitItem() as UiResult.Success).data.isStale)

            viewModel.onEvent(LuasForecastTabContract.Event.OnShowNotificationsDialog(5, "Broombridge"))
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
        // When
        viewModel.onEvent(LuasForecastTabContract.Event.OnShowTravelUpdatesDialog)

        // Then
        viewModel.uiResult.test {
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
    fun `timer should stop when all minutes are elapsed`() = runTest {
        // Given
        val dueInMins = 2
        val destination = "Broombridge"
        every { canScheduleExactAlarmsUseCase() } returns true

        // First setup notification state
        viewModel.onEvent(
            LuasForecastTabContract.Event.OnShowNotificationsDialog(
                dueInMins = dueInMins,
                destination = destination
            )
        )

        // When
        viewModel.onEvent(LuasForecastTabContract.Event.OnStartNotification(minutes = 1))

        // Check initial state
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            assertEquals(dueInMins, (result as UiResult.Success).data.luasNotificationState.dueInMins)
        }

        // Advance past timer end (2 minutes plus a bit more)
        advanceTimeBy(2 * 60_000 + 1000)
        runCurrent()

        // Then - timer should have stopped at 0
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            assertEquals(0, (result as UiResult.Success).data.luasNotificationState.dueInMins)
        }

        // Advance more time - should still be at 0
        advanceTimeBy(60_000)
        runCurrent()

        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            assertEquals(0, (result as UiResult.Success).data.luasNotificationState.dueInMins)
        }
    }

    @Test
    fun `Notification dialog is not displayed when exact alarm permission is disabled`() = runTest {
        // Given
        val dueInMins = 2
        val destination = "Broombridge"
        every { canScheduleExactAlarmsUseCase() } returns false
        every { requestExactAlarmPermissionUseCase() } returns Unit

        // First setup notification state
        viewModel.onEvent(
            LuasForecastTabContract.Event.OnShowNotificationsDialog(
                dueInMins = dueInMins,
                destination = destination
            )
        )

        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            assertEquals(LuasForecastTabDialogState.None, (result as UiResult.Success).data.dialog)
        }
        verify { requestExactAlarmPermissionUseCase() }
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
    fun `timer should be cancelled when OnStopNotification event is received`() = runTest {
        // Given
        val dueInMins = 10
        val destination = "Broombridge"
        every { canScheduleExactAlarmsUseCase() } returns true

        // Setup notification and start timer
        viewModel.onEvent(
            LuasForecastTabContract.Event.OnShowNotificationsDialog(
                dueInMins = dueInMins,
                destination = destination
            )
        )
        viewModel.onEvent(LuasForecastTabContract.Event.OnStartNotification(minutes = 5))

        // Advance time to verify the timer is running
        advanceTimeBy(60_000) // 1 minute
        runCurrent()

        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            assertEquals(9, (result as UiResult.Success).data.luasNotificationState.dueInMins)
        }

        // Stop the notification
        viewModel.onEvent(LuasForecastTabContract.Event.OnStopNotification)

        // Call to cancel alarm should be made
        verify { cancelAlarmUseCase() }
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
    fun `OnShowNotificationsDialog with zero or negative minutes should not show dialog`() = runTest {
        // Given
        val dueInMins = 0 // Zero minutes
        val destination = "Broombridge"

        // When
        viewModel.onEvent(
            LuasForecastTabContract.Event.OnShowNotificationsDialog(
                dueInMins = dueInMins,
                destination = destination
            )
        )

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            assertEquals(LuasForecastTabDialogState.None, (result as UiResult.Success).data.dialog)
        }

        // Try with negative minutes
        viewModel.onEvent(
            LuasForecastTabContract.Event.OnShowNotificationsDialog(
                dueInMins = -5,
                destination = destination
            )
        )

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            assertEquals(LuasForecastTabDialogState.None, (result as UiResult.Success).data.dialog)
        }
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
}
