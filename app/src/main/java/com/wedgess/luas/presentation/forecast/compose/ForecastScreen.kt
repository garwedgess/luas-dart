package com.wedgess.luas.presentation.forecast.compose

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.forecast.dart.compose.DartForecastContent
import com.wedgess.luas.presentation.forecast.luastab.compose.LuasForecastContent
import com.wedgess.luas.presentation.navigation.reminder.ReminderTarget
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun ForecastScreen(
    transportType: TransportType,
    setRefreshAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    reminderTarget: ReminderTarget? = null,
    onReminderComplete: (String, Boolean) -> Unit = { _, _ -> }
) {
    Surface(modifier) {
        when (transportType) {
            TransportType.LUAS -> LuasForecastContent(
                setRefreshAction = setRefreshAction,
                reminderTarget = reminderTarget,
                onReminderComplete = onReminderComplete
            )
            TransportType.DART -> DartForecastContent(
                setRefreshAction = setRefreshAction,
                reminderTarget = reminderTarget,
                onReminderComplete = onReminderComplete
            )
        }
    }
}

@Preview
@Composable
private fun ForecastScreenPreview() {
    LuasTheme {
        Surface {
            ForecastScreen(transportType = TransportType.LUAS, setRefreshAction = {})
        }
    }
}
