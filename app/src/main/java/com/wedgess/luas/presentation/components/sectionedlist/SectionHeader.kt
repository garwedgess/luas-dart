package com.wedgess.luas.presentation.components.sectionedlist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R

@Composable
fun SectionHeader(
    title: String,
    subTitle: String?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    showIcon: Boolean,
    modifier: Modifier = Modifier,
    connected: Boolean = false
) {
    val rotation by animateFloatAsState(if (isExpanded) 0f else COLLAPSED_ROTATION, label = "Direction expansion")
    val expansionState = stringResource(if (isExpanded) R.string.expanded else R.string.collapsed)
    Surface(
        modifier = modifier.fillMaxWidth().padding(bottom = if (connected) 0.dp else 14.dp)
            .semantics(mergeDescendants = true) { stateDescription = expansionState }
            .then(if (showIcon) Modifier.clickable(onClick = onToggle) else Modifier),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = if (connected) 0.dp else 20.dp,
            bottomEnd = if (connected) 0.dp else 20.dp
        )
    ) {
        Row(
            modifier = Modifier.heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val outward = title.equals("Outbound", true) || title.equals("Northbound", true)
            Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) {
                Icon(
                    if (outward) Icons.Filled.NorthEast else Icons.Filled.SouthEast,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp).size(24.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                subTitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            if (showIcon) Icon(Icons.Default.KeyboardArrowUp, null, Modifier.rotate(rotation))
        }
    }
}

private const val COLLAPSED_ROTATION = -180f
