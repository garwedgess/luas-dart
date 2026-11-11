package com.wedgess.luas.presentation.forecast.luastab.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wedgess.luas.presentation.components.AnimatedTabContainer
import com.wedgess.luas.presentation.forecast.compose.ForecastRefreshCycle
import com.wedgess.luas.presentation.forecast.luastab.compose.components.LuasForecastTabContent
import com.wedgess.luas.presentation.forecast.luastab.model.LuasForecastTab
import com.wedgess.luas.presentation.navigation.reminder.ReminderTarget
import kotlinx.collections.immutable.persistentListOf

@Composable
fun LuasForecastContent(
    setRefreshAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    reminderTarget: ReminderTarget? = null,
    onReminderComplete: (String, Boolean) -> Unit = { _, _ -> },
    onRefreshCycleChange: (ForecastRefreshCycle?) -> Unit = {}
) {
    val luasForecastTabs = remember { LuasForecastTab.all() }
    var currentTab by remember { mutableStateOf(luasForecastTabs.first()) }

    Column {
        AnimatedTabContainer(
            modifier = modifier,
            tabItems = luasForecastTabs,
            selectedPage = reminderTarget?.line?.let { line -> luasForecastTabs.indexOfFirst { it.line == line } },
            indicatorColors = persistentListOf(Color(0xFF66BF63), Color(0xFFE53935)),
            onTabIndexChange = { index -> currentTab = luasForecastTabs[index] }
        ) { tab ->
            LuasForecastTabContent(
                line = tab.line,
                reminderTarget = reminderTarget?.takeIf { it.line == tab.line },
                onReminderComplete = onReminderComplete,
                isActive = tab == currentTab,
                onRefreshAction = setRefreshAction,
                onRefreshCycleChange = onRefreshCycleChange
            )
        }
    }
}
