package com.wedgess.luas.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.ui.theme.LuasTheme

/**
 * The soonest departure, shown prominently. When the data is not live it shows the absolute [expectedTime] instead of
 * a countdown, matching the rows below it.
 */
@Composable
fun NextDepartureHeader(
    destination: String,
    dueMins: Int,
    expectedTime: String,
    stale: Boolean,
    modifier: Modifier = Modifier
) {
    val dueText = when {
        stale -> stringResource(R.string.label_expected_at, expectedTime)
        dueMins == 0 -> stringResource(R.string.now)
        else -> pluralStringResource(R.plurals.dart_minutes, dueMins, dueMins)
    }
    val description = if (stale) {
        stringResource(R.string.next_departure_description_stale, destination, expectedTime)
    } else {
        stringResource(R.string.next_departure_description_live, destination, dueText)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.next_departure_label), style = MaterialTheme.typography.labelMedium)
                Text(
                    destination,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Text(dueText, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Preview
@Composable
private fun NextDepartureHeaderPreview() {
    LuasTheme {
        Surface {
            NextDepartureHeader(destination = "Brides Glen", dueMins = 4, expectedTime = "09:14", stale = false)
        }
    }
}
