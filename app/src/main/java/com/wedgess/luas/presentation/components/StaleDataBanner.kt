package com.wedgess.luas.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.presentation.extensions.toClockTime
import com.wedgess.luas.ui.theme.LuasTheme
import java.time.Instant

/** Shown while the latest refresh has failed and the list is the last live result rather than current data. */
@Composable
fun StaleDataBanner(lastUpdated: Instant, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            text = stringResource(R.string.forecast_stale_message, lastUpdated.toClockTime()),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview
@Composable
private fun StaleDataBannerPreview() {
    LuasTheme {
        Surface {
            StaleDataBanner(lastUpdated = Instant.parse("2026-11-09T22:36:00Z"))
        }
    }
}
