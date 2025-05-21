package com.wedgess.luas.presentation.map.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.domain.model.UserLocation
import com.wedgess.luas.domain.usecase.FetchAllStopsUseCase
import com.wedgess.luas.domain.usecase.FetchCurrentLocationUseCase
import com.wedgess.luas.domain.usecase.IsLocationPermissionIgnoredUseCase
import com.wedgess.luas.domain.usecase.UpdateIgnoreLocationPermissionUseCase
import com.wedgess.luas.domain.usecase.UpdateLocationPermissionRequestedUseCase
import com.wedgess.luas.domain.usecase.WasLocationPermissionRequestedUseCase
import com.wedgess.luas.presentation.extensions.toPermission
import com.wedgess.luas.presentation.map.MapContract
import com.wedgess.luas.presentation.map.model.MapDialogState
import com.wedgess.luas.presentation.model.Permission
import com.wedgess.luas.presentation.model.UiResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalPermissionsApi::class)
@ExperimentalCoroutinesApi
class MapViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @MockK
    private lateinit var fetchCurrentLocationUseCase: FetchCurrentLocationUseCase

    @MockK
    private lateinit var fetchAllStopsUseCase: FetchAllStopsUseCase

    @MockK
    lateinit var isLocationPermissionIgnoredUseCase: IsLocationPermissionIgnoredUseCase

    @MockK
    lateinit var wasLocationPermissionRequestedUseCase: WasLocationPermissionRequestedUseCase

    @MockK
    lateinit var updateLocationPermissionRequestedUseCase: UpdateLocationPermissionRequestedUseCase

    @MockK
    lateinit var updateIgnoreLocationPermissionUseCase: UpdateIgnoreLocationPermissionUseCase

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
        // default behaviour for new use‑cases
        coEvery { isLocationPermissionIgnoredUseCase() } returns false
        coEvery { wasLocationPermissionRequestedUseCase() } returns false
        coEvery { updateLocationPermissionRequestedUseCase(any()) } returns Result.success(Unit)
        coEvery { updateIgnoreLocationPermissionUseCase(any()) } returns Result.success(Unit)
        every { fetchCurrentLocationUseCase.refresh() } returns true

        viewModel = MapViewModel(
            fetchCurrentLocationUseCase,
            fetchAllStopsUseCase,
            isLocationPermissionIgnoredUseCase,
            wasLocationPermissionRequestedUseCase,
            updateLocationPermissionRequestedUseCase,
            updateIgnoreLocationPermissionUseCase
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

    @Test
    fun `OnIgnoreLocationPermissionClick sets ignore flag`() = runTest {
        viewModel.onEvent(MapContract.Event.OnIgnoreLocationPermissionClick)
        coVerify { updateIgnoreLocationPermissionUseCase(true) }
    }

    @Test
    fun `OnLocationWasRequested persists requested flag`() = runTest {
        viewModel.onEvent(MapContract.Event.OnLocationWasRequested)
        coVerify { updateLocationPermissionRequestedUseCase(true) }
    }

    @Test
    fun `OnAcceptPermissionClick should clear dialog and emit ShowSystemLocationPermissionDialog effect`() = runTest {
        // Act
        viewModel.onEvent(MapContract.Event.OnAcceptPermissionClick)

        // Verify dialog is cleared and side effect is emitted
        viewModel.uiResult.test {
            val result = awaitItem()
            val state = (result as UiResult.Success).data
            assert(state.dialogState == MapDialogState.None)
            val effect = viewModel.sideEffect.first()

            assertTrue(effect is MapContract.Effect.ShowSystemLocationPermissionDialog)
        }
    }

    @Test
    fun `OnPermissionStateChanged should update permission state to Granted and refresh location`() = runTest {
        // Setup
        val mockPermissionState = mockk<MultiplePermissionsState>().apply {
            every { this@apply.allPermissionsGranted } returns true
        }
        coEvery { wasLocationPermissionRequestedUseCase() } returns true
        coEvery { isLocationPermissionIgnoredUseCase() } returns false
        every { mockPermissionState.toPermission(true) } returns Permission.Granted

        // Act
        viewModel.onEvent(MapContract.Event.OnPermissionStateChanged(mockPermissionState))

        // Verify
        coVerify { fetchCurrentLocationUseCase.refresh() }
        viewModel.uiResult.test {
            val result = awaitItem()
            val state = (result as UiResult.Success).data
            assert(state.locationPermission == Permission.Granted)
        }
    }

    @Test
    fun `OnPermissionStateChanged should update permission state to ShowRationale and show rationale dialog`() =
        runTest {
            // Setup
            val mockPermissionState = mockk<MultiplePermissionsState>(relaxed = true)

            // Mock properties to ensure toPermission returns ShowRationale
            every { mockPermissionState.allPermissionsGranted } returns false
            every { mockPermissionState.shouldShowRationale } returns true
            every { mockPermissionState.permissions } returns listOf(
                mockk<PermissionState>().apply {
                    every { this@apply.status } returns mockk<PermissionStatus.Denied>().apply status@{
                        every { this@status.shouldShowRationale } returns true
                        every { this@status.isGranted } returns false
                    }
                }
            )
            every { mockPermissionState.revokedPermissions } returns listOf(mockk(relaxed = true))

            coEvery { wasLocationPermissionRequestedUseCase() } returns true
            coEvery { isLocationPermissionIgnoredUseCase() } returns false

            // Act
            viewModel.onEvent(MapContract.Event.OnPermissionStateChanged(mockPermissionState))

            // Verify
            viewModel.uiResult.test {
                val result = awaitItem()
                val state = (result as UiResult.Success).data
                assert(state.locationPermission == Permission.ShowRationale)
                assert(state.dialogState == MapDialogState.LocationPermissionRationale)
            }
        }

    @Test
    fun `OnPermissionStateChanged should update permission state to PermanentlyDenied and show dialog when not ignored`() =
        runTest {
            // Setup
            val mockPermissionState = mockk<MultiplePermissionsState>(relaxed = true)

            // Mock properties to ensure toPermission returns PermanentlyDenied
            every { mockPermissionState.allPermissionsGranted } returns false
            every { mockPermissionState.shouldShowRationale } returns false
            every { mockPermissionState.permissions } returns listOf(
                mockk<PermissionState>().apply {
                    every { this@apply.status } returns mockk<PermissionStatus.Denied>().apply status@{
                        every { this@status.shouldShowRationale } returns false
                        every { this@status.isGranted } returns false
                    }
                }
            )
            every { mockPermissionState.revokedPermissions } returns listOf(mockk(relaxed = true))

            coEvery { wasLocationPermissionRequestedUseCase() } returns true
            coEvery { isLocationPermissionIgnoredUseCase() } returns false

            // Act
            viewModel.onEvent(MapContract.Event.OnPermissionStateChanged(mockPermissionState))

            // Verify
            viewModel.uiResult.test {
                val result = awaitItem()
                val state = (result as UiResult.Success).data
                assert(state.locationPermission == Permission.PermanentlyDenied)
                assert(state.dialogState == MapDialogState.LocationPermissionPermanentlyDenied)
            }
        }

    @Test
    fun `OnPermissionStateChanged should update permission state to PermanentlyDenied and hide dialog when ignored`() =
        runTest {
            // Setup
            val mockPermissionState = mockk<MultiplePermissionsState>(relaxed = true)

            // Mock properties to ensure toPermission returns PermanentlyDenied
            every { mockPermissionState.allPermissionsGranted } returns false
            every { mockPermissionState.shouldShowRationale } returns false
            every { mockPermissionState.permissions } returns listOf(
                mockk<PermissionState>().apply {
                    every { this@apply.status } returns mockk<PermissionStatus.Denied>().apply status@{
                        every { this@status.shouldShowRationale } returns false
                        every { this@status.isGranted } returns false
                    }
                }
            )
            every { mockPermissionState.revokedPermissions } returns listOf(mockk(relaxed = true))

            coEvery { wasLocationPermissionRequestedUseCase() } returns true
            coEvery { isLocationPermissionIgnoredUseCase() } returns true

            // Act
            viewModel.onEvent(MapContract.Event.OnPermissionStateChanged(mockPermissionState))

            // Verify
            viewModel.uiResult.test {
                val result = awaitItem()
                val state = (result as UiResult.Success).data
                assert(state.locationPermission == Permission.PermanentlyDenied)
                assert(state.dialogState == MapDialogState.None)
            }
        }

    @Test
    fun `OnDismissPermissionClick should clear dialog and set permission to Denied`() = runTest {
        // Act
        viewModel.onEvent(MapContract.Event.OnDismissPermissionClick)

        // Verify
        viewModel.uiResult.test {
            val result = awaitItem()
            val state = (result as UiResult.Success).data
            assert(state.dialogState == MapDialogState.None)
            assert(state.locationPermission == Permission.Denied)
        }
    }

    @Test
    fun `OnLocationPermanentlyDeniedDialog should show permanently denied dialog`() = runTest {
        // Act
        viewModel.onEvent(MapContract.Event.OnLocationPermanentlyDeniedDialog)

        // Verify
        viewModel.uiResult.test {
            val result = awaitItem()
            val state = (result as UiResult.Success).data
            assert(state.dialogState == MapDialogState.LocationPermissionPermanentlyDenied)
        }
    }

    @Test
    fun `OnDismissDialogClick should clear dialog state`() = runTest {
        // Setup - first set a dialog state
        viewModel.onEvent(MapContract.Event.OnLocationPermanentlyDeniedDialog)

        // Act
        viewModel.onEvent(MapContract.Event.OnDismissDialogClick)

        // Verify
        viewModel.uiResult.test {
            val result = awaitItem()
            val state = (result as UiResult.Success).data
            assert(state.dialogState == MapDialogState.None)
        }
    }

    @Test
    fun `OnOpenAppSettingsPermissionClick should clear dialog and emit OpenAppPermissionScreen effect`() = runTest {
        // Act
        viewModel.onEvent(MapContract.Event.OnOpenAppSettingsPermissionClick)

        // Verify
        viewModel.uiResult.test {
            val result = awaitItem()
            val state = (result as UiResult.Success).data
            assert(state.dialogState == MapDialogState.None)
            val effect = viewModel.sideEffect.first()
            assertTrue(effect is MapContract.Effect.OpenAppPermissionScreen)
        }
    }

    @Test
    fun `when ignoreLocationPermission is true and permission is granted, it should set ignore to false`() = runTest {
        // Setup
        val mockPermissionState = mockk<MultiplePermissionsState>(relaxed = true)

        // Mock properties to ensure toPermission returns Granted
        every { mockPermissionState.allPermissionsGranted } returns true
        every { mockPermissionState.shouldShowRationale } returns false
        every { mockPermissionState.permissions } returns emptyList()
        every { mockPermissionState.revokedPermissions } returns emptyList()

        coEvery { wasLocationPermissionRequestedUseCase() } returns true
        coEvery { isLocationPermissionIgnoredUseCase() } returns true

        // Act
        viewModel.onEvent(MapContract.Event.OnPermissionStateChanged(mockPermissionState))

        // Verify
        coVerify { updateIgnoreLocationPermissionUseCase(false) }
        coVerify { fetchCurrentLocationUseCase.refresh() }
    }
}
