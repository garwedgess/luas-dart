package com.wedgess.luas.presentation.forecast.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.wedgess.luas.domain.usecase.CanScheduleExactAlarmsUseCase
import com.wedgess.luas.domain.usecase.IsNotificationPermissionIgnoredUseCase
import com.wedgess.luas.domain.usecase.RequestExactAlarmPermissionUseCase
import com.wedgess.luas.domain.usecase.UpdateIgnoreNotificationPermissionUseCase
import com.wedgess.luas.domain.usecase.UpdateNotificationPermissionRequestedUseCase
import com.wedgess.luas.domain.usecase.WasNotificationPermissionRequestedUseCase
import com.wedgess.luas.presentation.base.SideEffectViewModel
import com.wedgess.luas.presentation.base.SideEffectViewModelImpl
import com.wedgess.luas.presentation.extensions.toPermission
import com.wedgess.luas.presentation.forecast.ForecastContract
import com.wedgess.luas.presentation.forecast.model.ForecastDialogState
import com.wedgess.luas.presentation.model.Permission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val isNotificationPermissionIgnoredUseCase: IsNotificationPermissionIgnoredUseCase,
    private val wasNotificationPermissionRequestedUseCase: WasNotificationPermissionRequestedUseCase,
    private val updateNotificationPermissionRequestedUseCase: UpdateNotificationPermissionRequestedUseCase,
    private val updateIgnoreNotificationPermissionUseCase: UpdateIgnoreNotificationPermissionUseCase,
    private val canScheduleExactAlarmsUseCase: CanScheduleExactAlarmsUseCase,
    private val requestExactAlarmPermissionUseCase: RequestExactAlarmPermissionUseCase
) : ViewModel(),
    SideEffectViewModel<ForecastContract.Effect> by SideEffectViewModelImpl() {

    private val initialState = ForecastContract.UiState()

    private val _uiState = MutableStateFlow(initialState)

    val uiState = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState)

    @OptIn(ExperimentalPermissionsApi::class)
    fun onEvent(event: ForecastContract.Event) {
        when (event) {
            ForecastContract.Event.OnAcceptPermissionClick -> {
                _uiState.update {
                    it.copy(dialog = ForecastDialogState.None)
                }.also {
                    viewModelScope.emitSideEffect(ForecastContract.Effect.ShowSystemNotificationPermissionDialog)
                }
            }

            is ForecastContract.Event.OnPermissionStateChanged -> handlePermissionChange(event.state)

            ForecastContract.Event.OnDismissPermissionClick -> _uiState.update {
                it.copy(dialog = ForecastDialogState.None, notificationPermission = Permission.Denied)
            }

            ForecastContract.Event.OnNotificationPermanentlyDeniedDialog -> _uiState.update {
                it.copy(dialog = ForecastDialogState.NotificationPermissionPermanentlyDenied)
            }

            ForecastContract.Event.OnDismissDialogClick -> _uiState.update {
                it.copy(dialog = ForecastDialogState.None)
            }

            ForecastContract.Event.OnIgnoreNotificationPermissionClick -> _uiState.update {
                it.copy(dialog = ForecastDialogState.None)
            }.also {
                viewModelScope.launch {
                    updateIgnoreNotificationPermissionUseCase(true)
                }
            }

            ForecastContract.Event.OnOpenAppSettingsPermissionClick -> _uiState.update {
                it.copy(dialog = ForecastDialogState.None)
            }.also {
                viewModelScope.emitSideEffect(ForecastContract.Effect.OpenAppPermissionScreen)
            }

            ForecastContract.Event.OnNotificationWasRequested -> viewModelScope.launch {
                updateNotificationPermissionRequestedUseCase(true)
            }

            ForecastContract.Event.OnOpenScheduleExactAlarmPermissionClick -> openExactAlarmPermission()
        }
    }

    private fun openExactAlarmPermission() {
        requestExactAlarmPermissionUseCase().also {
            _uiState.update {
                it.copy(dialog = ForecastDialogState.None)
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    private fun handlePermissionChange(permissionState: PermissionState) {
        viewModelScope.launch {
            val wasNotificationPermissionRequested = wasNotificationPermissionRequestedUseCase()
            val ignoreNotificationPermission = isNotificationPermissionIgnoredUseCase()
            val permission = permissionState.toPermission(wasNotificationPermissionRequested)
            if (permission == Permission.Granted && ignoreNotificationPermission) {
                updateIgnoreNotificationPermissionUseCase(false)
            }
            Timber.d(
                "Notification, wasNotificationPermissionRequested: $wasNotificationPermissionRequested, " +
                    "ignoreNotificationPermission: $ignoreNotificationPermission, " +
                    "permission: $permission"
            )
            _uiState.update {
                it.copy(
                    notificationPermission = permission,
                    dialog = when (permission) {
                        Permission.ShowRationale -> ForecastDialogState.NotificationPermissionRationale
                        Permission.PermanentlyDenied -> {
                            if (ignoreNotificationPermission) {
                                ForecastDialogState.None
                            } else {
                                ForecastDialogState.NotificationPermissionPermanentlyDenied
                            }
                        }

                        Permission.Granted -> {
                            if (!canScheduleExactAlarmsUseCase()) {
                                ForecastDialogState.ScheduleExactAlarms
                            } else {
                                it.dialog
                            }
                        }

                        else -> it.dialog
                    }
                )
            }
        }
    }
}
