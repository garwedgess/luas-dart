package com.wedgess.luas.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.wedgess.luas.presentation.forecast.compose.navigation.forecastRoot
import com.wedgess.luas.presentation.main.model.TopAppBarState
import com.wedgess.luas.presentation.map.compose.navigation.mapRoot
import com.wedgess.luas.presentation.navigation.reminder.ReminderTarget
import com.wedgess.luas.presentation.news.navigation.newsRoot
import com.wedgess.luas.presentation.timetable.compose.timetableRoot

@Composable
fun MainNavigationGraph(
    navController: NavHostController,
    onUpdateAppbarState: (TopAppBarState) -> Unit,
    modifier: Modifier = Modifier,
    reminderTarget: ReminderTarget? = null,
    onReminderComplete: (String, Boolean) -> Unit = { _, _ -> }
) {
    NavHost(
        navController = navController,
        startDestination = Screens.Forecast,
        modifier = modifier
    ) {
        forecastRoot(onUpdateAppbarState, reminderTarget, onReminderComplete)
        mapRoot(onUpdateAppbarState)
        newsRoot(onUpdateAppbarState)
        timetableRoot(onUpdateAppbarState)
    }
}
