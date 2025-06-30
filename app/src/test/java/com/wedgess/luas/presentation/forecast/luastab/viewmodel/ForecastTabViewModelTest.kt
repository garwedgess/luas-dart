package com.wedgess.luas.presentation.forecast.luastab.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.wedgess.luas.domain.model.LuasForecastEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.model.RefreshMode
import com.wedgess.luas.domain.model.RefreshState
import com.wedgess.luas.domain.usecase.CanScheduleExactAlarmsUseCase
import com.wedgess.luas.domain.usecase.CancelAlarmUseCase
import com.wedgess.luas.domain.usecase.FetchIsAlarmRunningUseCase
import com.wedgess.luas.domain.usecase.FetchLuasStopForecastUseCase
import com.wedgess.luas.domain.usecase.FetchLuasStopsUseCase
import com.wedgess.luas.domain.usecase.FetchSelectedLuasStopUseCase
import com.wedgess.luas.domain.usecase.RequestExactAlarmPermissionUseCase
import com.wedgess.luas.domain.usecase.ScheduleAlarmUseCase
import com.wedgess.luas.domain.usecase.UpdateSelectedLuasStopUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@ExperimentalCoroutinesApi
class ForecastTabViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val luasLine = LuasLineEntity.GREEN

    @MockK
    private lateinit var fetchLuasStopsUseCase: FetchLuasStopsUseCase

    @MockK
    private lateinit var fetchLuasStopForecastUseCase: FetchLuasStopForecastUseCase

    @MockK
    private lateinit var updateSelectedLuasStopUseCase: UpdateSelectedLuasStopUseCase

    @MockK
    private lateinit var fetchSelectedLuasStopUseCase: FetchSelectedLuasStopUseCase

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
            isCycleAndRide = false,
        ),
        LuasStopEntity(
            id = UUID.randomUUID(),
            abbreviation = "HAR",
            name = "Harcourt",
            latitude = 53.33334,
            longitude = -6.26302,
            line = LuasLineEntity.GREEN,
            isParkAndRide = false,
            isCycleAndRide = false,
        ),
    )

    private val mockForecast = LuasForecastEntity(
        message = "Trams operating normally",
        stop = "St. Stephens Green",
        createdAt = "2025-03-02T21:48:50",
        inboundTrams = emptyList(),
        outboundTrams = emptyList(),
        stopAbv = "STS",
    )

    private val stopsFlow = MutableStateFlow(Result.success(mockStops))
    private val forecastFlow = MutableStateFlow<RefreshState<LuasForecastEntity>>(RefreshState.Success(mockForecast, 0f))
    private val selectedStationFlow = MutableStateFlow("")
    private val isAlarmRunningFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { fetchLuasStopsUseCase(any()) } returns stopsFlow
        every { fetchLuasStopForecastUseCase(any()) } returns forecastFlow
        every { fetchSelectedLuasStopUseCase(any()) } returns selectedStationFlow
        every { isAlarmRunningUseCase() } returns isAlarmRunningFlow
        coEvery { updateSelectedLuasStopUseCase(any(), any()) } returns Result.success(Unit)
        every { canScheduleExactAlarmsUseCase() } returns false
        every { cancelAlarmUseCase() } returns Unit
        coEvery { scheduleAlarmUseCase(any(), any()) } returns Result.success(0L)

        viewModel = LuasForecastTabViewModel(
            luasLine = luasLine,
            fetchLuasStopsUseCase = fetchLuasStopsUseCase,
            fetchLuasStopForecastUseCase = fetchLuasStopForecastUseCase,
            updateSelectedLuasStopUseCase = updateSelectedLuasStopUseCase,
            canScheduleExactAlarmsUseCase = canScheduleExactAlarmsUseCase,
            cancelAlarmUseCase = cancelAlarmUseCase,
            scheduleAlarmUseCase = scheduleAlarmUseCase,
            isAlarmRunningUseCase = isAlarmRunningUseCase,
            fetchSelectedLuasStopUseCase = fetchSelectedLuasStopUseCase,
            requestExactAlarmPermissionUseCase = requestExactAlarmPermissionUseCase,
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
            assertEquals(mockStops.first().abbreviation, state.selectedStop.abbreviation)
        }
    }

    @Test
    fun `should find and select matching stop from selected station abbreviation`() = runTest {
        // Given
        selectedStationFlow.value = "HAR"

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            assertEquals("HAR", state.selectedStop.abbreviation)
            assertEquals("Harcourt", state.selectedStop.name)
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
            assertEquals("", state.selectedStop.abbreviation)
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
        forecastFlow.value = RefreshState.Error(Exception(errorMsg))

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Error)
            assertEquals(errorMsg, (result as UiResult.Error).message)
        }
    }

    @Test
    fun `should update progress in UI state when forecast refresh is in progress`() = runTest {
        // Given
        forecastFlow.value = RefreshState.Success(mockForecast, 0.5f)

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            assertEquals(0.5f, (result as UiResult.Success).data.refreshProgress)
        }
    }

    @Test
    fun `should update selected stop when OnStopSelected event is received`() = runTest {
        // Given
        val selectedStop = mockStops[1] // Harcourt

        // When
        viewModel.onEvent(LuasForecastTabContract.Event.OnStopSelected(selectedStop))

        // Then
        coVerify { updateSelectedLuasStopUseCase(selectedStop.abbreviation, selectedStop.line) }
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
    fun `should trigger manual refresh when OnRefresh event is received`() = runTest {
        every { fetchLuasStopForecastUseCase.refresh(any()) } returns Unit
        // When
        viewModel.onEvent(LuasForecastTabContract.Event.OnRefresh)

        // Then
        verify { fetchLuasStopForecastUseCase.refresh(RefreshMode.MANUAL) }
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
                destination = destination,
            ),
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
                destination = destination,
            ),
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
            isCycleAndRide = false,
        )

        // When
        viewModel.onEvent(LuasForecastTabContract.Event.OnStopSelected(nonExistentStop))

        // Then should still attempt to update the selected station
        coVerify { updateSelectedLuasStopUseCase(nonExistentStop.abbreviation, nonExistentStop.line) }
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
                destination = destination,
            ),
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
        coEvery { updateSelectedLuasStopUseCase(any(), any()) } returns Result.failure(Exception("Database error"))

        // When - should not crash
        viewModel.onEvent(LuasForecastTabContract.Event.OnStopSelected(mockStops[1]))

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
                destination = destination,
            ),
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
                destination = destination,
            ),
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
        forecastFlow.value = RefreshState.Error(Exception()) // No message

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Error)
            assertEquals("Failed to fetch forecast", (result as UiResult.Error).message)
        }
    }
}
