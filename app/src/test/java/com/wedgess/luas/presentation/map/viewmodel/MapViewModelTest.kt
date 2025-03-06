package com.wedgess.luas.presentation.map.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.domain.model.UserLocation
import com.wedgess.luas.domain.usecase.FetchAllStopsUseCase
import com.wedgess.luas.domain.usecase.FetchCurrentLocationUseCase
import com.wedgess.luas.presentation.model.UiResult
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
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
class MapViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @MockK
    private lateinit var fetchCurrentLocationUseCase: FetchCurrentLocationUseCase

    @MockK
    private lateinit var fetchAllStopsUseCase: FetchAllStopsUseCase

    private lateinit var viewModel: MapViewModel

    private val mockGreenLineStops = listOf(
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

    private val mockRedLineStops = listOf(
        StopEntity(
            id = UUID.randomUUID(),
            abbreviation = "ABB",
            name = "Abbey Street",
            latitude = 53.34835,
            longitude = -6.25786,
            line = LuasLineEntity.RED,
            isParkAndRide = false,
            isCycleAndRide = false
        ),
        StopEntity(
            id = UUID.randomUUID(),
            abbreviation = "JER",
            name = "Jervis",
            latitude = 53.34743,
            longitude = -6.26690,
            line = LuasLineEntity.RED,
            isParkAndRide = false,
            isCycleAndRide = false
        )
    )

    private val mockUserLocation = UserLocation(53.33963, -6.26070)
    private val locationFlow = MutableStateFlow<UserLocation?>(mockUserLocation)
    private val stopsFlow = MutableStateFlow(Result.success(mockGreenLineStops + mockRedLineStops))

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { fetchCurrentLocationUseCase() } returns locationFlow
        every { fetchAllStopsUseCase() } returns stopsFlow

        viewModel = MapViewModel(fetchCurrentLocationUseCase, fetchAllStopsUseCase)
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
    fun `should emit Success with user location and stops when data is available`() = runTest {
        viewModel.uiResult.test {
            val result = awaitItem()
            assert(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            assert(state.currentLocation == mockUserLocation)
            assert(state.greenLineLocations.size == 2)
            assert(state.redLineLocations.size == 2)
        }
    }

    @Test
    fun `should filter stops by line correctly`() = runTest {
        viewModel.uiResult.test {
            val result = awaitItem()
            val state = (result as UiResult.Success).data

            // Verify that all green line stops are in greenLineLocations
            assert(state.greenLineLocations.all { it.line == LuasLineEntity.GREEN })
            assert(state.greenLineLocations.size == mockGreenLineStops.size)

            // Verify that all red line stops are in redLineLocations
            assert(state.redLineLocations.all { it.line == LuasLineEntity.RED })
            assert(state.redLineLocations.size == mockRedLineStops.size)
        }
    }

    @Test
    fun `should use default location when current location is null`() = runTest {
        locationFlow.value = null

        viewModel.uiResult.test {
            val result = awaitItem()
            val state = (result as UiResult.Success).data
            assert(state.currentLocation == UserLocation(0.0, 0.0))
        }
    }

    @Test
    fun `should handle empty stops list`() = runTest {
        stopsFlow.value = Result.success(emptyList())

        viewModel.uiResult.test {
            val result = awaitItem()
            val state = (result as UiResult.Success).data
            assert(state.greenLineLocations.isEmpty())
            assert(state.redLineLocations.isEmpty())
        }
    }

    @Test
    fun `should handle stops fetch failure`() = runTest {
        stopsFlow.value = Result.failure(Exception("Failed to fetch stops"))

        viewModel.uiResult.test {
            val result = awaitItem()
            val state = (result as UiResult.Success).data
            assert(state.greenLineLocations.isEmpty())
            assert(state.redLineLocations.isEmpty())
        }
    }

    @Test
    fun `should update when location changes`() = runTest {
        val newLocation = UserLocation(53.35, -6.27)
        locationFlow.value = newLocation

        viewModel.uiResult.test {
            val result = awaitItem()
            val state = (result as UiResult.Success).data
            assert(state.currentLocation == newLocation)
        }
    }

    @Test
    fun `should update when stops change`() = runTest {
        val newStops = listOf(
            StopEntity(
                id = UUID.randomUUID(),
                abbreviation = "NEW",
                name = "New Stop",
                latitude = 53.35,
                longitude = -6.27,
                line = LuasLineEntity.GREEN,
                isParkAndRide = true,
                isCycleAndRide = true
            )
        )

        stopsFlow.value = Result.success(newStops)

        viewModel.uiResult.test {
            val result = awaitItem()
            val state = (result as UiResult.Success).data
            assert(state.greenLineLocations.size == 1)
            assert(state.greenLineLocations.first().abbreviation == "NEW")
            assert(state.redLineLocations.isEmpty())
        }
    }
}
