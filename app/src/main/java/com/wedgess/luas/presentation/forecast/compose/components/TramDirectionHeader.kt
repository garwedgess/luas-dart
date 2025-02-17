package com.wedgess.luas.presentation.forecast.compose.components

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
fun TramDirectionHeader(
    title: String,
    noTramsDue: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(8.dp),
            text = title,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium
        )
        AnimatedVisibility(noTramsDue) {
            Text(
                text = "No trams forecasted",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview
@Composable
private fun TramDirectionHeaderPreview(
    @PreviewParameter(TramDirectionHeaderPreviewProvider::class) param: Boolean
) {
    LuasTheme {
        Surface {
            TramDirectionHeader(
                title = stringResource(R.string.forecast_title_outbound),
                noTramsDue = param
            )
        }
    }
}

private class TramDirectionHeaderPreviewProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean>
        get() = sequenceOf(
            true,
            false
        )
}
