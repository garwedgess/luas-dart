package com.wedgess.luas.presentation.forecast.luastab.compose.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
fun LuasForecastItemRow(
    dueIn: Int,
    destination: String,
    modifier: Modifier = Modifier,
    stale: Boolean = false,
    expectedTime: String? = null,
    reminderSet: Boolean = false,
    onRowClick: (Int, String) -> Unit
) {
    val animatedValue by animateIntAsState(
        targetValue = dueIn,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "Animated Number"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !stale) { onRowClick(dueIn, destination) }
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = destination,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
        )
        if (stale && expectedTime != null) {
            Text(stringResource(R.string.label_expected_at, expectedTime), style = MaterialTheme.typography.labelLarge)
        } else if (!stale && dueIn >= 0) {
            DepartureCountdown(animatedValue)
        }
        if (reminderSet || (!stale && dueIn >= 0)) {
            DepartureReminderIcon(reminderSet)
        }
    }
}

@Preview
@Composable
private fun LuasForecastItemRowPreview(
    @PreviewParameter(LuasForecastItemRowPreviewParam::class) params: Pair<Int, String>
) {
    val (dueIn, destination) = params
    LuasTheme {
        Surface {
            LuasForecastItemRow(
                dueIn = dueIn,
                destination = destination,
                onRowClick = { _, _ -> }
            )
        }
    }
}

private class LuasForecastItemRowPreviewParam : PreviewParameterProvider<Pair<Int, String>> {
    override val values: Sequence<Pair<Int, String>>
        get() = sequenceOf(
            Pair(10, "Brides Glenn"),
            Pair(0, "Carrickmines")
        )
}
