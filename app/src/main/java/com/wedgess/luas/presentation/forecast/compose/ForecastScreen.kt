package com.wedgess.luas.presentation.forecast.compose

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.wedgess.luas.presentation.components.AnimatedTabContainer
import com.wedgess.luas.presentation.components.RequestNotificationPermission
import com.wedgess.luas.presentation.forecast.compose.components.ForecastTabContent
import com.wedgess.luas.presentation.forecast.model.ForecastTab
import com.wedgess.luas.ui.theme.LuasTheme
import kotlinx.collections.immutable.persistentListOf
import okhttp3.internal.toImmutableList

@Composable
fun ForecastScreen(
    setRefreshAction: (() -> Unit) -> Unit,
    onProgressChange: (Float) -> Unit
) {
    val forecastTabs = remember { ForecastTab.all() }
    val refreshActions = remember { mutableStateMapOf<ForecastTab, (() -> Unit)?>() }
    var currentTab by remember { mutableStateOf(forecastTabs.first()) }
    val currentRefreshAction by remember { derivedStateOf { refreshActions[currentTab] } }

    LaunchedEffect(currentTab, currentRefreshAction) {
        currentRefreshAction?.let(setRefreshAction)
    }

    RequestNotificationPermission()

    Surface {
        AnimatedTabContainer(
            tabItems = forecastTabs,
            indicatorColors = persistentListOf(Color(0xFF66BF63), Color(0xFFE53935)),
            onTabIndexChange = { index -> currentTab = forecastTabs[index] }
        ) { tab ->
            ForecastTabContent(
                line = tab.line,
                onRefreshAction = { action -> refreshActions.putIfAbsent(tab, action) },
                onProgressChange = { progress -> if (tab == currentTab) onProgressChange(progress) }
            )
        }
    }
}

@Preview
@Composable
private fun ForecastScreenPreview() {
    LuasTheme {
        Surface {
            ForecastScreen(setRefreshAction = {}, onProgressChange = {})
        }
    }
}
