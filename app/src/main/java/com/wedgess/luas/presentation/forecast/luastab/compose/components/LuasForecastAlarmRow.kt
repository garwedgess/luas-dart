package com.wedgess.luas.presentation.forecast.luastab.compose.components

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
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.extensions.toClockTime
import com.wedgess.luas.ui.theme.LuasTheme
import java.time.Instant

/** One active reminder. It shows the absolute time it was planned around, never a countdown that could go stale. */
@Composable
fun LuasForecastAlarmRow(
    alarm: Alarm,
    modifier: Modifier = Modifier,
    onCancelAlarm: (String) -> Unit
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
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                text = stringResource(
                    R.string.alarm_banner_msg,
                    alarm.stationName,
                    alarm.destination.orEmpty(),
                    alarm.notifyMinutesBefore,
                    alarm.targetExpectedAt?.toClockTime().orEmpty()
                ),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Start
            )
            IconButton(onClick = { onCancelAlarm(alarm.id) }) {
                Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null)
            }
        }
    }
}

@Preview
@Composable
private fun LuasForecastAlarmRowPreview() {
    LuasTheme {
        Surface {
            LuasForecastAlarmRow(
                alarm = Alarm(
                    id = "preview",
                    requestCode = 1,
                    kind = AlarmKind.SPECIFIC_DEPARTURE,
                    state = AlarmState.ACTIVE,
                    transport = TransportType.LUAS,
                    stationCode = "STS",
                    stationName = "St. Stephen's Green",
                    destination = "Carickmines",
                    notifyMinutesBefore = 10,
                    targetExpectedAt = Instant.parse("2026-11-09T22:30:00Z"),
                    reminderAt = Instant.parse("2026-11-09T22:19:30Z"),
                    createdAt = Instant.parse("2026-11-09T22:00:00Z"),
                    updatedAt = Instant.parse("2026-11-09T22:00:00Z")
                ),
                onCancelAlarm = {}
            )
        }
    }
}
