package com.wedgess.luas.presentation.forecast.tab.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun ForecastStatusMessage(
    message: String,
    modifier: Modifier = Modifier,
    showTravelUpdatesDialog: () -> Unit
) {
    val normalOperation = message.contains("operating normally")
    val color = if (normalOperation) {
        Color(0xFF228A43)
    } else {
        Color(0xFF996600)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = color),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = message.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    text = message,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
            AnimatedVisibility(visible = !normalOperation && message.isNotBlank()) {
                IconButton(onClick = showTravelUpdatesDialog) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Travel Updates",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ForecastStatusMessagePreview(
    @PreviewParameter(ForecastStatusMessagePreviewProvider::class) message: String
) {
    LuasTheme {
        Surface {
            ForecastStatusMessage(message, showTravelUpdatesDialog = {})
        }
    }
}

private class ForecastStatusMessagePreviewProvider : PreviewParameterProvider<String> {
    override val values: Sequence<String>
        get() = sequenceOf(
            "Services operating normally",
            "Services operating with delays see more infowmation on our website",
            ""
        )
}
