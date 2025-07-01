package com.wedgess.luas.presentation.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.wedgess.luas.domain.model.TransportType

@Composable
fun BottomNavigationBar(
    selectedTransportType: TransportType,
    onNavigateTo: (route: Screens) -> Unit,
    modifier: Modifier = Modifier,
    selectedItemRoute: String? = null
) {
    NavigationBar(modifier = modifier) {
        BottomNavItem.all(transportType = selectedTransportType).forEach { item ->
            val isSelected by remember(selectedItemRoute) {
                derivedStateOf { selectedItemRoute == item.route::class.qualifiedName }
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateTo(item.route) },
                label = { Text(text = item.title.asString()) },
                icon = {
                    if (item is BottomNavItem.Forecast) {
                        Icon(item.dynamicIcon(), contentDescription = item.title.asString())
                    } else {
                        Icon(item.icon, contentDescription = item.title.asString())
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.6f
                    ),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.6f
                    ),
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
