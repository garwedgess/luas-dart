package com.wedgess.luas.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun EmptyContent(
    title: String,
    modifier: Modifier = Modifier,
    subTitle: String? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(56.dp),
            imageVector = Icons.Outlined.Clear,
            contentDescription = "empty",
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        subTitle?.run {
            Text(text = this, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview
@Composable
private fun EmptyContentPreview() {
    Surface {
        EmptyContent(
            modifier = Modifier.fillMaxSize(),
            title = "No data found",
            subTitle = "No profile date for user"
        )
    }
}
