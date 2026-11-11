package com.wedgess.luas.presentation.forecast.dart.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wedgess.luas.R
import com.wedgess.luas.ui.theme.TransitDelayRed
import kotlin.math.abs

@Composable
fun LateWarningBadge(
    lateMinutes: Int,
    modifier: Modifier = Modifier
) {
    val description = pluralStringResource(
        if (lateMinutes >= 0) R.plurals.minutes_late else R.plurals.minutes_early,
        abs(lateMinutes),
        abs(lateMinutes)
    )
    Box(
        modifier = modifier.clearAndSetSemantics { contentDescription = description }
            .background(
                color = if (lateMinutes >= 0) {
                    TransitDelayRed
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${if (lateMinutes > 0) "+" else "−"}${abs(lateMinutes)} min",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = if (lateMinutes >= 0) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onTertiaryContainer
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Preview
@Composable
private fun LateWarningBadgePreview() {
    Surface {
        LateWarningBadge(modifier = Modifier.size(24.dp), lateMinutes = 13)
    }
}
