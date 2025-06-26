package com.wedgess.luas.presentation.forecast.dart.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun DartForecastItemRow(
    destination: String,
    scheduledTime: String,
    eta: String,
    dueIn: String,
    late: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.weight(0.4f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(0.8f),
                text = destination,
                style = MaterialTheme.typography.bodyMedium,
            )
            AnimatedVisibility(modifier = Modifier.weight(0.2f), visible = late != 0) {
                LateWarningBadge(late, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            modifier = Modifier.weight(0.2f),
            text = scheduledTime,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            modifier = Modifier.weight(0.2f),
            text = eta,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            modifier = Modifier.weight(0.2f),
            text = dueIn,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview
@Composable
private fun DartForecastItemRowPreview() {
    LuasTheme {
        Surface {
            Column {
                DartForecastItemRow(
                    destination = "Greystones",
                    scheduledTime = "16:34",
                    eta = "16.35",
                    dueIn = "3 min",
                    late = 3,
                )
            }
        }
    }
}
