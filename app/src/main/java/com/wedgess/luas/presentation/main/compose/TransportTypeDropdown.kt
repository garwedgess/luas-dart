package com.wedgess.luas.presentation.main.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.domain.model.TransportType

@Composable
fun TransportTypeDropdown(
    selectedTransportType: TransportType,
    modifier: Modifier = Modifier,
    onTransportTypeChange: (TransportType) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { isDropdownExpanded = !isDropdownExpanded }
        ) {
            Icon(
                painter = selectedTransportType.image(),
                contentDescription = "Transport options"
            )
        }

        DropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false }
        ) {
            TransportType.entries.forEach { transportType ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = transportType == selectedTransportType,
                                onClick = {
                                    onTransportTypeChange(transportType)
                                    isDropdownExpanded = false
                                }
                            )
                            Icon(painter = transportType.image(), contentDescription = transportType.name)
                            Text(transportType.name)
                        }
                    },
                    onClick = {
                        onTransportTypeChange(transportType)
                        isDropdownExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TransportType.image() = when (this) {
    TransportType.LUAS -> painterResource(R.drawable.ic_tram)
    TransportType.DART -> painterResource(R.drawable.ic_train)
}
