package com.wedgess.luas.presentation.forecast.tab.compose.components

import androidx.compose.runtime.Composable
import com.wedgess.luas.presentation.forecast.tab.ForecastTabContract
import com.wedgess.luas.presentation.forecast.tab.compose.dialogs.NotificationTimeDialog
import com.wedgess.luas.presentation.forecast.tab.compose.dialogs.TravelUpdatesDialog
import com.wedgess.luas.presentation.forecast.tab.model.ForecastTabDialogState
import com.wedgess.luas.presentation.forecast.tab.model.NotificationState

@Composable
fun ForecastTabDialogs(
    dialogsState: ForecastTabDialogState,
    notificationState: NotificationState,
    onEvent: (ForecastTabContract.Event) -> Unit
) {
    when (dialogsState) {
        is ForecastTabDialogState.None -> Unit
        is ForecastTabDialogState.TravelUpdatesAlert -> TravelUpdatesDialog(
            onDismiss = { onEvent(ForecastTabContract.Event.OnDismissDialog) }
        )

        is ForecastTabDialogState.Notification -> NotificationTimeDialog(
            currentMinutes = notificationState.notifyMinutesBefore,
            dueInMinutes = notificationState.dueInMins,
            onMinutesChange = { onEvent(ForecastTabContract.Event.OnNotificationMinutesChanged(it)) },
            onConfirm = { minutesBefore ->
                onEvent(
                    ForecastTabContract.Event.OnStartNotification(minutes = minutesBefore)
                )
            },
            onDismiss = { onEvent(ForecastTabContract.Event.OnDismissDialog) }
        )
    }
}
