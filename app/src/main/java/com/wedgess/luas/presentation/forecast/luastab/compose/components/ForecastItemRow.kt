package com.wedgess.luas.presentation.forecast.luastab.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R

@Composable
fun ForecastItemRow(
    dueInMins: Int,
    destination: String,
    modifier: Modifier = Modifier,
    onRowClick: (Int, String) -> Unit
) {
    val animatedValue by animateIntAsState(
        targetValue = dueInMins,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "Animated Number"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onRowClick(dueInMins, destination) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AnimatedVisibility(visible = animatedValue != -1) {
            Text(
                text = if (animatedValue == 0) {
                    stringResource(R.string.label_due_now_to)
                } else {
                    stringResource(R.string.label_due_in_mins_placeholder, animatedValue)
                },
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            text = destination,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}
