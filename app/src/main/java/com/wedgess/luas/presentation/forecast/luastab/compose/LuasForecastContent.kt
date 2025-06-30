package com.wedgess.luas.presentation.forecast.luastab.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wedgess.luas.presentation.components.AnimatedTabContainer
import com.wedgess.luas.presentation.forecast.compose.RefreshProgressIndicator
import com.wedgess.luas.presentation.forecast.luastab.compose.components.LuasForecastTabContent
import com.wedgess.luas.presentation.forecast.luastab.model.LuasForecastTab
import kotlinx.collections.immutable.persistentListOf

@Composable
fun LuasForecastContent(
    setRefreshAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val luasForecastTabs = remember { LuasForecastTab.all() }
    var currentTab by remember { mutableStateOf(luasForecastTabs.first()) }
    var refreshProgress by remember { mutableFloatStateOf(0f) }

    Column {
        RefreshProgressIndicator(refreshProgress = refreshProgress)
        AnimatedTabContainer(
            modifier = modifier,
            tabItems = luasForecastTabs,
            indicatorColors = persistentListOf(Color(0xFF66BF63), Color(0xFFE53935)),
            onTabIndexChange = { index -> currentTab = luasForecastTabs[index] }
        ) { tab ->
            LuasForecastTabContent(
                line = tab.line,
                onRefreshAction = { action -> setRefreshAction(action) },
                onProgressChange = { progress -> if (tab == currentTab) refreshProgress = progress }
            )
        }
    }
}
