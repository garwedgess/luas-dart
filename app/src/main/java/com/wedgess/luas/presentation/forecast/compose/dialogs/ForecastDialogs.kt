package com.wedgess.luas.presentation.forecast.compose.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.wedgess.luas.presentation.forecast.ForecastContract
import com.wedgess.luas.presentation.forecast.model.ForecastDialogState

@Composable
fun ForecastDialogs(
    dialogsState: ForecastDialogState,
    onEvent: (ForecastContract.Event) -> Unit,
) {
    when (dialogsState) {
        is ForecastDialogState.None -> Unit
        ForecastDialogState.NotificationPermissionRationale -> AlertDialog(
            onDismissRequest = { onEvent(ForecastContract.Event.OnDismissDialogClick) },
            title = { Text("Notification Permission") },
            text = { Text("Notification permission is required if you want to set notifications for Luas arrivals. If not, you can Deny the permission for now and if you wish to allow it in the future, then you can do so from your devices Settings") },
            confirmButton = {
                TextButton(onClick = { onEvent(ForecastContract.Event.OnAcceptPermissionClick) }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ForecastContract.Event.OnDismissDialogClick) }) {
                    Text("Deny")
                }
            },
        )

        ForecastDialogState.NotificationPermissionPermanentlyDenied -> AlertDialog(
            onDismissRequest = { onEvent(ForecastContract.Event.OnDismissDialogClick) },
            title = { Text("Location Permission") },
            text = { Text("Notification permission has been permanently denied, you must manually grant it from your devices Settings. Do you want to grant notification permission now?") },
            confirmButton = {
                TextButton(onClick = { onEvent(ForecastContract.Event.OnOpenAppSettingsPermissionClick) }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ForecastContract.Event.OnIgnoreNotificationPermissionClick) }) {
                    Text("No")
                }
            },
        )

        ForecastDialogState.ScheduleExactAlarms -> AlertDialog(
            onDismissRequest = { onEvent(ForecastContract.Event.OnDismissDialogClick) },
            title = { Text("Alarm Permission") },
            text = { Text("In order to show the notifications, the app requires to have permission to set exact alarms. You need to manually enable the permission, do you want to be taken to the permission screen now?") },
            confirmButton = {
                TextButton(onClick = { onEvent(ForecastContract.Event.OnOpenScheduleExactAlarmPermissionClick) }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ForecastContract.Event.OnDismissDialogClick) }) {
                    Text("No")
                }
            },
        )
    }
}
