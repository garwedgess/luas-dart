package com.wedgess.luas.presentation.forecast.dart.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.presentation.forecast.compose.DepartureCountdown
import com.wedgess.luas.presentation.forecast.compose.DepartureReminderIcon
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun DartForecastItemRow(
    destination: String,
    scheduledTime: String,
    eta: String,
    dueIn: Int,
    late: Int,
    modifier: Modifier = Modifier,
    showCountdown: Boolean = true,
    reminderSet: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val animatedValue by animateIntAsState(
        targetValue = dueIn,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "Animated Number"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClickLabel = stringResource(R.string.forecast_set_reminder),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .heightIn(min = 64.dp).padding(vertical = 8.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    destination,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (late != 0) LateWarningBadge(late)
            }
            Text(
                stringResource(R.string.forecast_scheduled_eta, scheduledTime, eta),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showCountdown) {
            DepartureCountdown(animatedValue)
        }
        if (reminderSet || showCountdown) {
            DepartureReminderIcon(reminderSet)
        }
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
                    dueIn = 3,
                    late = 3
                )
            }
        }
    }
}

@Preview
@Composable
private fun DartForecastItemRowPreview(
    @PreviewParameter(LuasForecastItemRowPreviewParam::class) params: Triple<Int, Int, String>
) {
    val (dueIn, late, destination) = params
    LuasTheme {
        Surface {
            DartForecastItemRow(
                dueIn = dueIn,
                destination = destination,
                late = late,
                scheduledTime = "16:34",
                eta = "16.35"
            )
        }
    }
}

private class LuasForecastItemRowPreviewParam : PreviewParameterProvider<Triple<Int, Int, String>> {
    override val values: Sequence<Triple<Int, Int, String>>
        get() = sequenceOf(
            Triple(10, 0, "Greystones"),
            Triple(2, 1, "Bray"),
            Triple(3, -2, "Killiney"),
            Triple(0, 0, "Dalkey")
        )
}
