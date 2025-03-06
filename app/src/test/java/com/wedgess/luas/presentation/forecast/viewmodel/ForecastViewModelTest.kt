package com.wedgess.luas.presentation.forecast.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.wedgess.luas.domain.model.ForcastEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.RefreshMode
import com.wedgess.luas.domain.model.RefreshState
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.domain.usecase.FetchForecastUseCase
import com.wedgess.luas.domain.usecase.FetchStopsUseCase
import com.wedgess.luas.presentation.forecast.ForecastContract
import com.wedgess.luas.presentation.forecast.model.ForecastDialogState
import com.wedgess.luas.presentation.model.UiResult
import io.mockk.MockKAnnotations
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
    private val forecastFlow =
        MutableStateFlow<RefreshState<ForcastEntity>>(RefreshState.Success(mockForecast, 0f))

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { fetchStopsUseCase(any()) } returns stopsFlow
        every { fetchForecastUseCase(any()) } returns forecastFlow

        viewModel = ForecastViewModel(luasLine, fetchStopsUseCase, fetchForecastUseCase)
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
    fun `should update selectedStop when OnStopSelected event is received`() = runTest {
        val secondStop = mockStops[1]
        viewModel.onEvent(ForecastContract.Event.OnStopSelected(secondStop))

        viewModel.uiResult.test {
            val result = awaitItem()
            assert((result as UiResult.Success).data.selectedStop == secondStop)
        }
    }

    @Test
    fun `should trigger manual refresh when OnRefresh event is received`() = runTest {
        every { fetchForecastUseCase.refresh(any()) } returns Unit
        viewModel.onEvent(ForecastContract.Event.OnRefresh)
        verify { fetchForecastUseCase.refresh(RefreshMode.MANUAL) }
    }

    @Test
    fun `should show travel updates dialog when OnShowTravelUpdatesDialog event is received`() =
        runTest {
            viewModel.onEvent(ForecastContract.Event.OnShowTravelUpdatesDialog)
            viewModel.uiResult.test {
                val result = awaitItem()
                assert((result as UiResult.Success).data.dialog == ForecastDialogState.TravelUpdatesAlert)
            }
        }

    @Test
    fun `should dismiss travel updates dialog when OnDismissTravelUpdatesDialog event is received`() =
        runTest {
            viewModel.onEvent(ForecastContract.Event.OnShowTravelUpdatesDialog)
            viewModel.onEvent(ForecastContract.Event.OnDismissTravelUpdatesDialog)
            viewModel.uiResult.test {
                val result = awaitItem()
                assert((result as UiResult.Success).data.dialog == ForecastDialogState.None)
            }
        }

    @Test
    fun `should use existing selectedStop when available`() = runTest {
        val secondStop = mockStops[1]
        viewModel.onEvent(ForecastContract.Event.OnStopSelected(secondStop))
        stopsFlow.value = Result.success(mockStops)
        viewModel.uiResult.test {
            val result = awaitItem()
            assert((result as UiResult.Success).data.selectedStop == secondStop)
        }
    }
}
