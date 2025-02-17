package com.wedgess.luas.presentation.forecast.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun ForecastStatusMessage(message: String) {
    val color = if (message.contains("operating normally")) {
        Color(0xFF228A43)
    } else {
        Color(0xFFBC8C00)
    }
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = color)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        text = message,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        textAlign = TextAlign.Center
    )
}

@Preview
@Composable
private fun ForecastStatusMessagePreview(
    @PreviewParameter(ForecastStatusMessagePreviewProvider::class) message: String
) {
    LuasTheme {
        Surface {
            ForecastStatusMessage(message)
        }
    }
}

private class ForecastStatusMessagePreviewProvider : PreviewParameterProvider<String> {
    override val values: Sequence<String>
        get() = sequenceOf(
            "Services operating normally",
            "Services operating with delays"
        )
}
