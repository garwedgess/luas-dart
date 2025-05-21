package com.wedgess.luas.presentation.forecast.tab.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.presentation.forecast.tab.model.NotificationState
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun ForecastAlarmRow(
    notificationState: NotificationState,
    modifier: Modifier = Modifier,
    onCancelAlarm: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                text = stringResource(
                    R.string.notifying_msg_for_to_in,
                    notificationState.station,
                    notificationState.destination,
                    notificationState.dueInMins
                ),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onCancelAlarm) {
                Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null)
            }
        }
    }
}

@Preview
@Composable
private fun ForecastAlarmRowPreview() {
    LuasTheme {
        Surface {
            ForecastAlarmRow(
                notificationState = NotificationState(
                    dueInMins = 10,
                    destination = "Carickmines",
                    station = "St. Stephen's Green",
                    notifyMinutesBefore = 10
                ),
                onCancelAlarm = {}
            )
        }
    }
}
