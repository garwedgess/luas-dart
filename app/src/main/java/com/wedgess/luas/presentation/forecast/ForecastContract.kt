package com.wedgess.luas.presentation.forecast

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.forecast.model.ForecastDialogState
import com.wedgess.luas.presentation.model.Permission

interface ForecastContract {

    data class UiState(
        val dialog: ForecastDialogState = ForecastDialogState.None,
        val notificationPermission: Permission = Permission.Unknown,
        val transportType: TransportType = TransportType.LUAS,
    )

    sealed interface Event {
        data object OnOpenScheduleExactAlarmPermissionClick : Event
        data object OnNotificationPermanentlyDeniedDialog : Event
        data object OnNotificationWasRequested : Event
        data object OnIgnoreNotificationPermissionClick : Event
        data object OnDismissPermissionClick : Event
        data object OnOpenAppSettingsPermissionClick : Event
        data object OnAcceptPermissionClick : Event
        data object OnDismissDialogClick : Event

        @OptIn(ExperimentalPermissionsApi::class)
        data class OnPermissionStateChanged(val state: PermissionState) : Event
    }

    sealed interface Effect {
        data object ShowSystemNotificationPermissionDialog : Effect
        data object OpenAppPermissionScreen : Effect
    }
}
