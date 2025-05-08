package com.wedgess.luas.presentation.forecast.compose.components

import androidx.compose.runtime.Composable
import com.wedgess.luas.presentation.forecast.ForecastContract
import com.wedgess.luas.presentation.forecast.compose.dialogs.NotificationTimeDialog
import com.wedgess.luas.presentation.forecast.compose.dialogs.TravelUpdatesDialog
import com.wedgess.luas.presentation.forecast.model.ForecastDialogState
import com.wedgess.luas.presentation.forecast.model.NotificationState

@Composable
fun ForecastDialogs(
    dialogsState: ForecastDialogState,
    notificationState: NotificationState,
    onEvent: (ForecastContract.Event) -> Unit,
) {
    when (dialogsState) {
        is ForecastDialogState.None -> Unit
        is ForecastDialogState.TravelUpdatesAlert -> TravelUpdatesDialog(
            onDismiss = { onEvent(ForecastContract.Event.OnDismissDialog) },
        )

        is ForecastDialogState.Notification -> NotificationTimeDialog(
            currentMinutes = notificationState.notifyMinutesBefore,
            dueInMinutes = notificationState.dueInMins,
            onMinutesChange = { onEvent(ForecastContract.Event.OnNotificationMinutesChanged(it)) },
            onConfirm = { minutesBefore ->
                onEvent(
                    ForecastContract.Event.OnStartNotification(minutes = minutesBefore),
                )
            },
            onDismiss = { onEvent(ForecastContract.Event.OnDismissDialog) },
        )
    }
}
