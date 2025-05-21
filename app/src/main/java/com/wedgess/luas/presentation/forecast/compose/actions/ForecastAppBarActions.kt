package com.wedgess.luas.presentation.forecast.compose.actions

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun ForecastAppBarActions(modifier: Modifier = Modifier, onRefresh: () -> Unit) {
    Row {
        IconButton(modifier = modifier, onClick = onRefresh) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
        }
    }
}

@Preview
@Composable
private fun ForecastAppBarActionsPreview() {
    LuasTheme {
        Surface {
            ForecastAppBarActions(onRefresh = {})
        }
    }
}
