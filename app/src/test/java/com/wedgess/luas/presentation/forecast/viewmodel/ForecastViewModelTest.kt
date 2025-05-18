package com.wedgess.luas.presentation.forecast.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.wedgess.luas.domain.usecase.CanScheduleExactAlarmsUseCase
import com.wedgess.luas.domain.usecase.IsNotificationPermissionIgnoredUseCase
import com.wedgess.luas.domain.usecase.RequestExactAlarmPermissionUseCase
import com.wedgess.luas.domain.usecase.UpdateIgnoreNotificationPermissionUseCase
import com.wedgess.luas.domain.usecase.UpdateNotificationPermissionRequestedUseCase
import com.wedgess.luas.domain.usecase.WasNotificationPermissionRequestedUseCase
import com.wedgess.luas.presentation.forecast.ForecastContract
import com.wedgess.luas.presentation.forecast.model.ForecastDialogState
import com.wedgess.luas.presentation.model.Permission
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

@OptIn(ExperimentalPermissionsApi::class)
@ExperimentalCoroutinesApi
class ForecastViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @MockK
    private lateinit var isNotificationPermissionIgnoredUseCase: IsNotificationPermissionIgnoredUseCase

    @MockK
    private lateinit var wasNotificationPermissionRequestedUseCase: WasNotificationPermissionRequestedUseCase

    @MockK
    private lateinit var updateNotificationPermissionRequestedUseCase: UpdateNotificationPermissionRequestedUseCase

    @MockK
    private lateinit var updateIgnoreNotificationPermissionUseCase: UpdateIgnoreNotificationPermissionUseCase

    @MockK
    private lateinit var canScheduleExactAlarmsUseCase: CanScheduleExactAlarmsUseCase

    @MockK
    private lateinit var requestExactAlarmPermissionUseCase: RequestExactAlarmPermissionUseCase

    private lateinit var viewModel: ForecastViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // Default behavior for use cases
        coEvery { isNotificationPermissionIgnoredUseCase() } returns false
        coEvery { wasNotificationPermissionRequestedUseCase() } returns false
        coEvery { updateNotificationPermissionRequestedUseCase(any()) } returns Result.success(Unit)
        coEvery { updateIgnoreNotificationPermissionUseCase(any()) } returns Result.success(Unit)
        coEvery { canScheduleExactAlarmsUseCase() } returns true
        coEvery { requestExactAlarmPermissionUseCase() } returns Unit

        viewModel = ForecastViewModel(
            isNotificationPermissionIgnoredUseCase,
            wasNotificationPermissionRequestedUseCase,
            updateNotificationPermissionRequestedUseCase,
            updateIgnoreNotificationPermissionUseCase,
            canScheduleExactAlarmsUseCase,
            requestExactAlarmPermissionUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have default values`() = runTest {
        val initialState = viewModel.uiState.value
        assertEquals(ForecastContract.UiState(), initialState)
        assertEquals(ForecastDialogState.None, initialState.dialog)
        assertEquals(Permission.Unknown, initialState.notificationPermission)
    }

    @Test
    fun `OnAcceptPermissionClick should clear dialog and emit ShowSystemNotificationPermissionDialog effect`() = runTest {
        // Act
        viewModel.onEvent(ForecastContract.Event.OnAcceptPermissionClick)

        // Assert
        assertEquals(ForecastDialogState.None, viewModel.uiState.value.dialog)

        val effect = viewModel.sideEffect.first()
        assertTrue(effect is ForecastContract.Effect.ShowSystemNotificationPermissionDialog)
    }

    @Test
    fun `OnDismissPermissionClick should clear dialog and set permission to Denied`() = runTest {
        // Act
        viewModel.onEvent(ForecastContract.Event.OnDismissPermissionClick)

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(Permission.Denied, state.notificationPermission)
            assertEquals(ForecastDialogState.None, state.dialog)
        }
    }

    @Test
    fun `OnNotificationPermanentlyDeniedDialog should show permanently denied dialog`() = runTest {
        // Act
        viewModel.onEvent(ForecastContract.Event.OnNotificationPermanentlyDeniedDialog)

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(ForecastDialogState.NotificationPermissionPermanentlyDenied, state.dialog)
        }
    }

    @Test
    fun `OnDismissDialogClick should clear dialog state`() = runTest {
        // Setup - first set a dialog state
        viewModel.onEvent(ForecastContract.Event.OnNotificationPermanentlyDeniedDialog)

        // Act
        viewModel.onEvent(ForecastContract.Event.OnDismissDialogClick)

        // Assert
        assertEquals(ForecastDialogState.None, viewModel.uiState.value.dialog)
    }

    @Test
    fun `OnIgnoreNotificationPermissionClick sets ignore flag and clears dialog`() = runTest {
        // Act
        viewModel.onEvent(ForecastContract.Event.OnIgnoreNotificationPermissionClick)

        // Assert
        assertEquals(ForecastDialogState.None, viewModel.uiState.value.dialog)
        coVerify { updateIgnoreNotificationPermissionUseCase(true) }
    }

    @Test
    fun `OnOpenAppSettingsPermissionClick should clear dialog and emit OpenAppPermissionScreen effect`() = runTest {
        // Act
        viewModel.onEvent(ForecastContract.Event.OnOpenAppSettingsPermissionClick)

        // Assert
        assertEquals(ForecastDialogState.None, viewModel.uiState.value.dialog)

        val effect = viewModel.sideEffect.first()
        assertTrue(effect is ForecastContract.Effect.OpenAppPermissionScreen)
    }

    @Test
    fun `OnNotificationWasRequested persists requested flag`() = runTest {
        // Act
        viewModel.onEvent(ForecastContract.Event.OnNotificationWasRequested)

        // Assert
        coVerify { updateNotificationPermissionRequestedUseCase(true) }
    }

    @Test
    fun `OnOpenScheduleExactAlarmPermissionClick requests exact alarm permission and clears dialog`() = runTest {
        // Act
        viewModel.onEvent(ForecastContract.Event.OnOpenScheduleExactAlarmPermissionClick)

        // Assert
        assertEquals(ForecastDialogState.None, viewModel.uiState.value.dialog)
        verify { requestExactAlarmPermissionUseCase() }
    }

    @Test
    fun `handlePermissionChange sets permission to ShowRationale and shows rationale dialog`() = runTest {
        // Setup
        val mockPermissionState = mockk<PermissionState>(relaxed = true)

        // Mock properties to ensure toPermission returns ShowRationale
        every { mockPermissionState.status } returns mockk<PermissionStatus.Denied>().apply {
            every { this@apply.shouldShowRationale } returns true
            every { this@apply.isGranted } returns false
        }

        coEvery { wasNotificationPermissionRequestedUseCase() } returns true
        coEvery { isNotificationPermissionIgnoredUseCase() } returns false

        // Act
        viewModel.onEvent(ForecastContract.Event.OnPermissionStateChanged(mockPermissionState))

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(Permission.ShowRationale, state.notificationPermission)
            assertEquals(ForecastDialogState.NotificationPermissionRationale, state.dialog)
        }
    }

    @Test
    fun `handlePermissionChange sets permission to PermanentlyDenied and shows dialog when not ignored`() = runTest {
        // Setup
        val mockPermissionState = mockk<PermissionState>(relaxed = true)

        // Mock properties to ensure toPermission returns PermanentlyDenied
        every { mockPermissionState.status } returns mockk<PermissionStatus.Denied>().apply {
            every { this@apply.shouldShowRationale } returns false
            every { this@apply.isGranted } returns false
        }

        coEvery { wasNotificationPermissionRequestedUseCase() } returns true
        coEvery { isNotificationPermissionIgnoredUseCase() } returns false

        // Act
        viewModel.onEvent(ForecastContract.Event.OnPermissionStateChanged(mockPermissionState))

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(Permission.PermanentlyDenied, state.notificationPermission)
            assertEquals(ForecastDialogState.NotificationPermissionPermanentlyDenied, state.dialog)
        }
    }

    @Test
    fun `handlePermissionChange sets permission to PermanentlyDenied but does not show dialog when ignored`() = runTest {
        // Setup
        val mockPermissionState = mockk<PermissionState>(relaxed = true)

        // Mock properties to ensure toPermission returns PermanentlyDenied
        every { mockPermissionState.status } returns mockk<PermissionStatus.Denied>().apply {
            every { this@apply.shouldShowRationale } returns false
            every { this@apply.isGranted } returns false
        }

        coEvery { wasNotificationPermissionRequestedUseCase() } returns true
        coEvery { isNotificationPermissionIgnoredUseCase() } returns true

        // Act
        viewModel.onEvent(ForecastContract.Event.OnPermissionStateChanged(mockPermissionState))

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(Permission.PermanentlyDenied, state.notificationPermission)
            assertEquals(ForecastDialogState.None, state.dialog)
        }
    }

    @Test
    fun `handlePermissionChange sets permission to Granted and shows ScheduleExactAlarms dialog when needed`() = runTest {
        // Setup
        val mockPermissionState = mockk<PermissionState>(relaxed = true)

        // Mock properties to ensure toPermission returns Granted
        every { mockPermissionState.status } returns mockk<PermissionStatus.Granted>().apply {
            every { this@apply.isGranted } returns true
        }

        coEvery { wasNotificationPermissionRequestedUseCase() } returns true
        coEvery { isNotificationPermissionIgnoredUseCase() } returns false
        coEvery { canScheduleExactAlarmsUseCase() } returns false

        // Act
        viewModel.onEvent(ForecastContract.Event.OnPermissionStateChanged(mockPermissionState))

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(Permission.Granted, state.notificationPermission)
            assertEquals(ForecastDialogState.ScheduleExactAlarms, state.dialog)
        }
    }

    @Test
    fun `handlePermissionChange sets permission to Granted and maintains dialog when exact alarms allowed`() = runTest {
        // Setup - First set a different dialog state
        viewModel.onEvent(ForecastContract.Event.OnNotificationPermanentlyDeniedDialog)

        val mockPermissionState = mockk<PermissionState>(relaxed = true)

        // Mock properties to ensure toPermission returns Granted
        every { mockPermissionState.status } returns mockk<PermissionStatus.Granted>().apply {
            every { this@apply.isGranted } returns true
        }

        coEvery { wasNotificationPermissionRequestedUseCase() } returns true
        coEvery { isNotificationPermissionIgnoredUseCase() } returns false
        coEvery { canScheduleExactAlarmsUseCase() } returns true

        // Act
        viewModel.onEvent(ForecastContract.Event.OnPermissionStateChanged(mockPermissionState))

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(Permission.Granted, state.notificationPermission)
            assertEquals(ForecastDialogState.NotificationPermissionPermanentlyDenied, state.dialog)
        }
    }

    @Test
    fun `when ignoreNotificationPermission is true and permission is granted, it should set ignore to false`() = runTest {
        // Setup
        val mockPermissionState = mockk<PermissionState>(relaxed = true)

        // Mock properties to ensure toPermission returns Granted
        every { mockPermissionState.status } returns mockk<PermissionStatus.Granted>().apply {
            every { this@apply.isGranted } returns true
        }

        coEvery { wasNotificationPermissionRequestedUseCase() } returns true
        coEvery { isNotificationPermissionIgnoredUseCase() } returns true
        coEvery { canScheduleExactAlarmsUseCase() } returns true

        // Act
        viewModel.onEvent(ForecastContract.Event.OnPermissionStateChanged(mockPermissionState))

        // Assert
        coVerify { updateIgnoreNotificationPermissionUseCase(false) }
    }
}
