package com.wedgess.luas.presentation.components.sectionedlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun SectionHeader(
    title: String,
    subTitle: String?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    showIcon: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -180f,
        animationSpec = tween(500),
        label = "Icon rotation"
    )

    Surface(
        modifier = modifier.clickable { onToggle() },
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                subTitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
            AnimatedVisibility(visible = showIcon) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isExpanded) "Collapse section" else "Expand section",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun SectionRowPreview() {
    LuasTheme {
        SectionHeader(title = "Title", subTitle = "Sub title", isExpanded = true, showIcon = true, onToggle = {})
    }
}
