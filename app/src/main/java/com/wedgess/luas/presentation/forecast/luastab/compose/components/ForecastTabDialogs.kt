package com.wedgess.luas.presentation.forecast.luastab.compose.components

import androidx.compose.runtime.Composable
import com.wedgess.luas.presentation.forecast.luastab.LuasForecastTabContract
import com.wedgess.luas.presentation.forecast.luastab.compose.dialogs.NotificationTimeDialog
import com.wedgess.luas.presentation.forecast.luastab.compose.dialogs.TravelUpdatesDialog
import com.wedgess.luas.presentation.forecast.luastab.model.ForecastTabDialogState
import com.wedgess.luas.presentation.forecast.luastab.model.NotificationState

@Composable
fun ForecastTabDialogs(
    dialogsState: ForecastTabDialogState,
    notificationState: NotificationState,
    onEvent: (LuasForecastTabContract.Event) -> Unit
) {
    when (dialogsState) {
        is ForecastTabDialogState.None -> Unit
        is ForecastTabDialogState.TravelUpdatesAlert -> TravelUpdatesDialog(
            onDismiss = { onEvent(LuasForecastTabContract.Event.OnDismissDialog) }
        )

        is ForecastTabDialogState.Notification -> NotificationTimeDialog(
            currentMinutes = notificationState.notifyMinutesBefore,
            dueInMinutes = notificationState.dueInMins,
            onMinutesChange = { onEvent(LuasForecastTabContract.Event.OnNotificationMinutesChanged(it)) },
            onConfirm = { minutesBefore ->
                onEvent(
                    LuasForecastTabContract.Event.OnStartNotification(minutes = minutesBefore)
                )
            },
            onDismiss = { onEvent(LuasForecastTabContract.Event.OnDismissDialog) }
        )
    }
}
