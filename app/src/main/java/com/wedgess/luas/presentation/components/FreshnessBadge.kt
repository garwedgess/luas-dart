package com.wedgess.luas.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.presentation.extensions.toClockTime
import com.wedgess.luas.ui.theme.LuasTheme
import java.time.Instant

/**
 * Shown only for live data: the time of the last successful fetch. Stale or stored data uses [StaleDataBanner]
 * instead, so the two are never shown together. The time is fixed, not a ticking age, so it can never go stale itself.
 */
@Composable
fun FreshnessBadge(fetchedAt: Instant, modifier: Modifier = Modifier) {
    val time = fetchedAt.toClockTime()
    val description = stringResource(R.string.freshness_live_description, time)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier.size(8.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.tertiary
        ) {}
        Text(
            text = stringResource(R.string.freshness_live, time),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Preview
@Composable
private fun FreshnessBadgePreview() {
    LuasTheme { Surface { FreshnessBadge(Instant.parse("2026-11-09T09:14:00Z")) } }
}
