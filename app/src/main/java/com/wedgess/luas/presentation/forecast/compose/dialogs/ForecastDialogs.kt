package com.wedgess.luas.presentation.forecast.compose.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wedgess.luas.R
import com.wedgess.luas.presentation.forecast.ForecastContract
import com.wedgess.luas.presentation.forecast.model.ForecastDialogState

@Composable
fun ForecastDialogs(
    dialogsState: ForecastDialogState,
    onEvent: (ForecastContract.Event) -> Unit
) {
    when (dialogsState) {
        is ForecastDialogState.None -> Unit
        ForecastDialogState.NotificationPermissionRationale -> AlertDialog(
            onDismissRequest = { onEvent(ForecastContract.Event.OnDismissDialogClick) },
            title = { Text(stringResource(R.string.notification_permission)) },
            text = { Text(stringResource(R.string.notification_permission_rationale_msg)) },
            confirmButton = {
                TextButton(onClick = { onEvent(ForecastContract.Event.OnAcceptPermissionClick) }) {
                    Text(stringResource(R.string.btn_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ForecastContract.Event.OnDismissDialogClick) }) {
                    Text(stringResource(R.string.btn_deny))
                }
            }
        )

        ForecastDialogState.NotificationPermissionPermanentlyDenied -> AlertDialog(
            onDismissRequest = { onEvent(ForecastContract.Event.OnDismissDialogClick) },
            title = { Text(stringResource(R.string.notification_permission)) },
            text = { Text(stringResource(R.string.notification_permission_permanently_denied_msg)) },
            confirmButton = {
                TextButton(onClick = { onEvent(ForecastContract.Event.OnOpenAppSettingsPermissionClick) }) {
                    Text(stringResource(R.string.btn_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ForecastContract.Event.OnIgnoreNotificationPermissionClick) }) {
                    Text(stringResource(R.string.btn_no))
                }
            }
        )

        ForecastDialogState.ScheduleExactAlarms -> AlertDialog(
            onDismissRequest = { onEvent(ForecastContract.Event.OnDismissDialogClick) },
            title = { Text(stringResource(R.string.alarm_permission_dialog_title)) },
            text = { Text(stringResource(R.string.exact_alarm_permission_msg)) },
            confirmButton = {
                TextButton(onClick = { onEvent(ForecastContract.Event.OnOpenScheduleExactAlarmPermissionClick) }) {
                    Text(stringResource(R.string.btn_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ForecastContract.Event.OnDismissDialogClick) }) {
                    Text(stringResource(R.string.btn_no))
                }
            }
        )
    }
}
