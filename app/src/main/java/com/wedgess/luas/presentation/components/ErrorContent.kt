package com.wedgess.luas.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ErrorContent(
    title: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    subTitle: String? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(72.dp),
            imageVector = Icons.Outlined.Info,
            contentDescription = "error",
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        subTitle?.run {
            Text(text = this, style = MaterialTheme.typography.bodyMedium)
        }

        onRetry?.run {
            Button(onClick = this@run) {
                Text(text = "Retry")
            }
        }
    }
}

@Preview
@Composable
private fun ErrorContentPreview() {
    Surface {
        ErrorContent(
            modifier = Modifier.fillMaxSize(),
            title = "Fetching info failed",
            subTitle = "Incorrect data provided",
            onRetry = {}
        )
    }
}
