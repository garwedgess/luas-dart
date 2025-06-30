package com.wedgess.luas.presentation.forecast.luastab.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun LuasDirectionHeader(
    title: String,
    noTramsDue: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                .padding(8.dp),
            text = title,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium,
        )
        AnimatedVisibility(noTramsDue) {
            Text(
                text = "No trams forecasted",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Preview
@Composable
private fun LuasDirectionHeaderPreview(
    @PreviewParameter(LuasDirectionHeaderPreviewProvider::class) param: Boolean,
) {
    LuasTheme {
        Surface {
            LuasDirectionHeader(
                title = stringResource(R.string.forecast_title_outbound),
                noTramsDue = param,
            )
        }
    }
}

private class LuasDirectionHeaderPreviewProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean>
        get() = sequenceOf(
            true,
            false,
        )
}
