package com.wedgess.luas.presentation.forecast.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.wedgess.luas.domain.model.ForcastEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.RefreshMode
import com.wedgess.luas.domain.model.RefreshState
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.domain.usecase.FetchForecastUseCase
import com.wedgess.luas.domain.usecase.FetchSelectedStationUseCase
import com.wedgess.luas.domain.usecase.FetchStopsUseCase
import com.wedgess.luas.domain.usecase.UpdateSelectedStationUseCase
import com.wedgess.luas.presentation.forecast.ForecastContract
import com.wedgess.luas.presentation.forecast.model.ForecastDialogState
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
import kotlinx.coroutines.test.resetMain
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
class ForecastViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val luasLine = LuasLineEntity.GREEN

    @MockK
    private lateinit var fetchStopsUseCase: FetchStopsUseCase

    @MockK
    private lateinit var fetchForecastUseCase: FetchForecastUseCase

    @MockK
    private lateinit var updateSelectedStationUseCase: UpdateSelectedStationUseCase

    @MockK
    private lateinit var fetchSelectedStationUseCase: FetchSelectedStationUseCase

    private lateinit var viewModel: ForecastViewModel

    private val mockStops = listOf(
        StopEntity(
            id = UUID.randomUUID(),
            abbreviation = "STA",
            name = "St. Stephens Green",
            latitude = 53.33963,
            longitude = -6.26070,
            line = LuasLineEntity.GREEN,
            isParkAndRide = false,
            isCycleAndRide = false
        ),
        StopEntity(
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

    private val mockForecast = ForcastEntity(
        message = "Trams operating normally",
        stop = "St. Stephens Green",
        createdAt = "2025-03-02T21:48:50",
        inboundTrams = emptyList(),
        outboundTrams = emptyList(),
        stopAbv = "STS"
    )

    private val stopsFlow = MutableStateFlow(Result.success(mockStops))
    private val forecastFlow = MutableStateFlow<RefreshState<ForcastEntity>>(RefreshState.Success(mockForecast, 0f))
    private val selectedStationFlow = MutableStateFlow("")

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { fetchStopsUseCase(any()) } returns stopsFlow
        every { fetchForecastUseCase(any()) } returns forecastFlow
        every { fetchSelectedStationUseCase(any()) } returns selectedStationFlow
        coEvery { updateSelectedStationUseCase(any(), any()) } returns Result.success(Unit)

        viewModel = ForecastViewModel(
            luasLine,
            fetchStopsUseCase,
            fetchForecastUseCase,
            updateSelectedStationUseCase,
            fetchSelectedStationUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be Loading`() = runTest {
        assert(viewModel.uiResult.value is UiResult.Loading)
    }

    @Test
    fun `should emit Success with stops and forecast when data is available`() = runTest {
        viewModel.uiResult.test {
            val result = awaitItem()
            assert(result is UiResult.Success)
        }
    }

    @Test
    fun `should emit Error when stops fetch fails`() = runTest {
        stopsFlow.value = Result.failure(Exception("Failed to fetch stops"))

        viewModel.uiResult.test {
            val result = awaitItem()
            assert(result is UiResult.Error)
        }
    }

    @Test
    fun `should emit Empty when stops list is empty`() = runTest {
        stopsFlow.value = Result.success(emptyList())
        viewModel.uiResult.test {
            val result = awaitItem()
            assert(result is UiResult.Empty)
        }
    }

    @Test
    fun `should emit Error when forecast fetch fails`() = runTest {
        forecastFlow.value = RefreshState.Error(Exception("Network error"))
        viewModel.uiResult.test {
            val result = awaitItem()
            assert(result is UiResult.Error)
        }
    }

    @Test
    fun `should update refreshProgress when forecast is refreshing`() = runTest {
        forecastFlow.value = RefreshState.Success(mockForecast, 50f)
        viewModel.uiResult.test {
            val result = awaitItem()
            assert((result as UiResult.Success).data.refreshProgress == 50f)
        }
    }

    @Test
    fun `should update selected station in repository when OnStopSelected event is received`() = runTest {
        val secondStop = mockStops[1]
        viewModel.onEvent(ForecastContract.Event.OnStopSelected(secondStop))

        coVerify { updateSelectedStationUseCase(secondStop.abbreviation, secondStop.line) }
    }

    @Test
    fun `should trigger manual refresh when OnRefresh event is received`() = runTest {
        every { fetchForecastUseCase.refresh(any()) } returns Unit
        viewModel.onEvent(ForecastContract.Event.OnRefresh)
        verify { fetchForecastUseCase.refresh(RefreshMode.MANUAL) }
    }

    @Test
    fun `should show travel updates dialog when OnShowTravelUpdatesDialog event is received`() = runTest {
        viewModel.onEvent(ForecastContract.Event.OnShowTravelUpdatesDialog)
        viewModel.uiResult.test {
            val result = awaitItem()
            assert((result as UiResult.Success).data.dialog == ForecastDialogState.TravelUpdatesAlert)
        }
    }

    @Test
    fun `should dismiss travel updates dialog when OnDismissTravelUpdatesDialog event is received`() = runTest {
        viewModel.onEvent(ForecastContract.Event.OnShowTravelUpdatesDialog)
        viewModel.onEvent(ForecastContract.Event.OnDismissTravelUpdatesDialog)
        viewModel.uiResult.test {
            val result = awaitItem()
            assert((result as UiResult.Success).data.dialog == ForecastDialogState.None)
        }
    }

    @Test
    fun `should select first stop when no saved selection exists`() = runTest {
        selectedStationFlow.value = ""
        stopsFlow.value = Result.success(mockStops)

        viewModel.uiResult.test {
            val result = awaitItem()
            assert((result as UiResult.Success).data.selectedStop == mockStops.first())
        }
    }

    @Test
    fun `should select saved stop when available`() = runTest {
        selectedStationFlow.value = "HAR"
        stopsFlow.value = Result.success(mockStops)

        viewModel.uiResult.test {
            val result = awaitItem()
            assert((result as UiResult.Success).data.selectedStop == mockStops[1])
        }
    }

    @Test
    fun `should use default stop when saved stop not found in list`() = runTest {
        selectedStationFlow.value = "INVALID"
        stopsFlow.value = Result.success(mockStops)

        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            assertEquals(
                StopEntity.initial().copy(id = (result as UiResult.Success).data.selectedStop.id),
                result.data.selectedStop
            )
        }
    }

    @Test
    fun `should update selected station in repository when stop changes`() = runTest {
        val secondStop = mockStops[1]

        viewModel.onEvent(ForecastContract.Event.OnStopSelected(secondStop))

        coVerify { updateSelectedStationUseCase(secondStop.abbreviation, secondStop.line) }
    }
}
