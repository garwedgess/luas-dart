package com.wedgess.luas.presentation.forecast.luastab.compose.components

import androidx.compose.runtime.Composable
import com.wedgess.luas.presentation.forecast.luastab.LuasForecastTabContract
import com.wedgess.luas.presentation.forecast.luastab.compose.dialogs.NotificationTimeDialog
import com.wedgess.luas.presentation.forecast.luastab.compose.dialogs.TravelUpdatesDialog
import com.wedgess.luas.presentation.forecast.luastab.model.LuasForecastTabDialogState
import com.wedgess.luas.presentation.forecast.luastab.model.LuasNotificationState

@Composable
fun LuasForecastTabDialogs(
    dialogsState: LuasForecastTabDialogState,
    luasNotificationState: LuasNotificationState,
    onEvent: (LuasForecastTabContract.Event) -> Unit
) {
    when (dialogsState) {
        is LuasForecastTabDialogState.None -> Unit
        is LuasForecastTabDialogState.TravelUpdatesAlert -> TravelUpdatesDialog(
            onDismiss = { onEvent(LuasForecastTabContract.Event.OnDismissDialog) }
        )

        is LuasForecastTabDialogState.Notification -> NotificationTimeDialog(
            currentMinutes = luasNotificationState.notifyMinutesBefore,
            dueInMinutes = luasNotificationState.dueInMins,
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
