package com.wedgess.luas.presentation.forecast.dart.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.wedgess.luas.domain.model.DartDirectionEntity
import com.wedgess.luas.domain.model.DartLocationTypeEntity
import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.domain.model.DartStationForecastEntity
import com.wedgess.luas.domain.model.RefreshMode
import com.wedgess.luas.domain.model.RefreshState
import com.wedgess.luas.domain.usecase.FetchAllDartStationsUseCase
import com.wedgess.luas.domain.usecase.FetchDartStationForecastUseCase
import com.wedgess.luas.domain.usecase.FetchSelectedDartStationUseCase
import com.wedgess.luas.domain.usecase.UpdateSelectedDartStationUseCase
import com.wedgess.luas.presentation.forecast.dart.DartForecastTabContract
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

@ExperimentalCoroutinesApi
class DartForecastViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @MockK
    private lateinit var fetchAllDartStationsUseCase: FetchAllDartStationsUseCase

    @MockK
    private lateinit var fetchDartStationForecastUseCase: FetchDartStationForecastUseCase

    @MockK
    private lateinit var updateSelectedDartStationsUseCase: UpdateSelectedDartStationUseCase

    @MockK
    private lateinit var fetchSelectedDartStationsUseCase: FetchSelectedDartStationUseCase

    private lateinit var viewModel: DartForecastViewModel

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

    private val mockForecasts = listOf(
        DartStationForecastEntity(
            serverTime = "2025-06-26T21:35:15.323",
            trainCode = "D123",
            stationFullName = "Dublin Connolly",
            stationCode = "CNLLY",
            queryTime = "21:35:15",
            trainDate = "02 Dec 2024",
            origin = "Howth",
            destination = "Bray",
            originTime = "10:00",
            destinationTime = "11:00",
            status = "On Time",
            lastLocation = "Howth Junction",
            dueIn = 5,
            late = 0,
            expArrival = "10:30",
            expDepart = "10:31",
            schArrival = "10:30",
            schDepart = "10:31",
            trainType = "DART",
            direction = DartDirectionEntity.SOUTHBOUND,
            locationType = DartLocationTypeEntity.STOP
        ),
        DartStationForecastEntity(
            serverTime = "2025-06-26T21:35:15.323",
            trainCode = "D456",
            stationFullName = "Dublin Connolly",
            stationCode = "CNLLY",
            queryTime = "21:35:15",
            trainDate = "02 Dec 2024",
            origin = "Bray",
            destination = "Howth",
            originTime = "10:15",
            destinationTime = "11:15",
            status = "On Time",
            lastLocation = "Tara Street",
            dueIn = 8,
            late = 0,
            expArrival = "10:45",
            expDepart = "10:46",
            schArrival = "10:45",
            schDepart = "10:46",
            trainType = "DART",
            direction = DartDirectionEntity.NORTHBOUND,
            locationType = DartLocationTypeEntity.STOP
        )
    )

    private val stationsFlow = MutableStateFlow(Result.success(mockStations))
    private val forecastFlow = MutableStateFlow<RefreshState<List<DartStationForecastEntity>>>(
        RefreshState.Success(mockForecasts, 0f)
    )
    private val selectedStationFlow = MutableStateFlow("")

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { fetchAllDartStationsUseCase() } returns stationsFlow
        every { fetchDartStationForecastUseCase(any()) } returns forecastFlow
        every { fetchSelectedDartStationsUseCase() } returns selectedStationFlow
        coEvery { updateSelectedDartStationsUseCase(any()) } returns Result.success(Unit)

        viewModel = DartForecastViewModel(
            fetchAllDartStationsUseCase = fetchAllDartStationsUseCase,
            fetchDartStationForecastUseCase = fetchDartStationForecastUseCase,
            updateSelectedDartStationsUseCase = updateSelectedDartStationsUseCase,
            fetchSelectedDartStationsUseCase = fetchSelectedDartStationsUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should select first station when no selected station is available`() = runTest {
        // Given
        selectedStationFlow.value = ""

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            assertEquals(mockStations.first().code, state.selectedStation.code)
        }
    }

    @Test
    fun `should find and select matching station from selected station code`() = runTest {
        // Given
        selectedStationFlow.value = "PERSE"

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            assertEquals("PERSE", state.selectedStation.code)
            assertEquals("Dublin Pearse", state.selectedStation.name)
        }
    }

    @Test
    fun `should use initial station when selected station not found in stations list`() = runTest {
        // Given
        selectedStationFlow.value = "NON_EXISTENT"

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            // Should default to DartStationEntity.initial()
            assertEquals("", state.selectedStation.code)
        }
    }

    @Test
    fun `should handle empty stations list gracefully`() = runTest {
        // Given
        stationsFlow.value = Result.success(emptyList())

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Empty)
            assertEquals("No stops found", (result as UiResult.Empty).message)
        }
    }

    @Test
    fun `should handle error when fetching stations`() = runTest {
        // Given
        val errorMsg = "Network error"
        stationsFlow.value = Result.failure(Exception(errorMsg))

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
        forecastFlow.value = RefreshState.Success(mockForecasts, 0.5f)

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            assertEquals(0.5f, (result as UiResult.Success).data.refreshProgress)
        }
    }

    @Test
    fun `should update selected station when OnStationSelected event is received`() = runTest {
        // Given
        val selectedStation = mockStations[1] // Dublin Pearse

        // When
        viewModel.onEvent(DartForecastTabContract.Event.OnStationSelected(selectedStation))

        // Then
        coVerify { updateSelectedDartStationsUseCase(selectedStation.code) }
    }

    @Test
    fun `should trigger manual refresh when OnRefresh event is received`() = runTest {
        every { fetchDartStationForecastUseCase.refresh(any()) } returns Unit

        // When
        viewModel.onEvent(DartForecastTabContract.Event.OnRefresh)

        // Then
        verify { fetchDartStationForecastUseCase.refresh(RefreshMode.MANUAL) }
    }

    @Test
    fun `should create sections for all directions even when no forecasts exist`() = runTest {
        // Given
        forecastFlow.value = RefreshState.Success(emptyList(), 0f)

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            val sections = state.sectionedListState.sections

            assertTrue(sections.any { it.title == DartDirectionEntity.NORTHBOUND.key })
            assertTrue(sections.any { it.title == DartDirectionEntity.SOUTHBOUND.key })
        }
    }

    @Test
    fun `should group forecasts by direction correctly`() = runTest {
        // Given
        forecastFlow.value = RefreshState.Success(mockForecasts, 0f)

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            val sections = state.sectionedListState.sections

            val northboundSection = sections.first { it.title == DartDirectionEntity.NORTHBOUND.key }
            val southboundSection = sections.first { it.title == DartDirectionEntity.SOUTHBOUND.key }

            assertEquals(1, northboundSection.items.size)
            assertEquals(1, southboundSection.items.size)
            assertEquals("1 train", northboundSection.subTitle)
            assertEquals("1 train", southboundSection.subTitle)
        }
    }

    @Test
    fun `should show correct subtitle for multiple trains`() = runTest {
        // Given
        val multipleTrains = mockForecasts + listOf(
            mockForecasts[0].copy(trainCode = "D789", dueIn = 12),
            mockForecasts[0].copy(trainCode = "D999", dueIn = 15)
        )
        forecastFlow.value = RefreshState.Success(multipleTrains, 0f)

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            val sections = state.sectionedListState.sections

            val southboundSection = sections.first { it.title == DartDirectionEntity.SOUTHBOUND.key }
            assertEquals("3 trains", southboundSection.subTitle)
        }
    }

    @Test
    fun `should show 'No trains' subtitle when direction has no forecasts`() = runTest {
        // Given - only southbound trains
        val southboundOnly = listOf(mockForecasts[0])
        forecastFlow.value = RefreshState.Success(southboundOnly, 0f)

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            val sections = state.sectionedListState.sections

            val northboundSection = sections.first { it.title == DartDirectionEntity.NORTHBOUND.key }
            assertEquals("No trains", northboundSection.subTitle)
        }
    }

    @Test
    fun `should toggle section expansion state when OnToggleSection event is received`() = runTest {
        // Given
        val sectionId = DartDirectionEntity.NORTHBOUND.id

        // When - toggle section
        viewModel.onEvent(DartForecastTabContract.Event.OnToggleSection(sectionId))

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            val expandedSections = state.sectionedListState.expandedSections

            // Should be false (collapsed) since default is true
            assertEquals(false, expandedSections[sectionId])
        }

        // When - toggle again
        viewModel.onEvent(DartForecastTabContract.Event.OnToggleSection(sectionId))

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            val expandedSections = state.sectionedListState.expandedSections

            // Should be true (expanded) again
            assertEquals(true, expandedSections[sectionId])
        }
    }

    @Test
    fun `should handle non-existent station when OnStationSelected is called`() = runTest {
        // Given a station that's not in the list
        val nonExistentStation = DartStationEntity(
            id = 999,
            name = "Non-existent Station",
            alias = "NonExistent",
            latitude = 0.0,
            longitude = 0.0,
            code = "NON"
        )

        // When
        viewModel.onEvent(DartForecastTabContract.Event.OnStationSelected(nonExistentStation))

        // Then should still attempt to update the selected station
        coVerify { updateSelectedDartStationsUseCase(nonExistentStation.code) }
    }

    @Test
    fun `should handle update of selected station failure gracefully`() = runTest {
        // Given
        coEvery { updateSelectedDartStationsUseCase(any()) } returns Result.failure(Exception("Database error"))

        // When - should not crash
        viewModel.onEvent(DartForecastTabContract.Event.OnStationSelected(mockStations[1]))

        // Then - test should complete without exceptions
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

    @Test
    fun `should handle null error message from stations exception`() = runTest {
        // Given
        stationsFlow.value = Result.failure(Exception()) // No message

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Error)
            assertEquals("Failed to fetch stations", (result as UiResult.Error).message)
        }
    }

    @Test
    fun `should map forecast data correctly to section row data`() = runTest {
        // Given
        val forecast = mockForecasts[0]
        forecastFlow.value = RefreshState.Success(listOf(forecast), 0f)

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            val sections = state.sectionedListState.sections

            val southboundSection = sections.first { it.title == DartDirectionEntity.SOUTHBOUND.key }
            val firstItem = southboundSection.items.first()

            assertEquals(forecast.trainCode, firstItem.content.trainCode)
            assertEquals(forecast.dueIn, firstItem.content.dueIn)
            assertEquals(forecast.destination, firstItem.content.destination)
            assertEquals(forecast.status, firstItem.content.status)
            assertEquals(forecast.lastLocation, firstItem.content.lastLocation)
            assertEquals(forecast.late, firstItem.content.late)
            assertEquals(forecast.direction, firstItem.content.direction)
        }
    }

    @Test
    fun `should use correct scheduled and expected times based on location type`() = runTest {
        // Given - Origin location type
        val originForecast = mockForecasts[0].copy(
            locationType = DartLocationTypeEntity.ORIGIN,
            schArrival = "10:00",
            schDepart = "10:01",
            expArrival = "10:02",
            expDepart = "10:03"
        )
        forecastFlow.value = RefreshState.Success(listOf(originForecast), 0f)

        // Then
        viewModel.uiResult.test {
            val result = awaitItem()
            assertTrue(result is UiResult.Success)
            val state = (result as UiResult.Success).data
            val sections = state.sectionedListState.sections

            val southboundSection = sections.first { it.title == DartDirectionEntity.SOUTHBOUND.key }
            val firstItem = southboundSection.items.first()

            // For ORIGIN, should use departure times
            assertEquals("10:01", firstItem.content.scheduledAt)
            assertEquals("10:03", firstItem.content.expectedAt)
        }
    }
}
