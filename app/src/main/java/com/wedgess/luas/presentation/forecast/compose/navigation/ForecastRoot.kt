package com.wedgess.luas.presentation.forecast.compose.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.wedgess.luas.presentation.forecast.compose.ForecastScreen
import com.wedgess.luas.presentation.forecast.compose.actions.ForecastAppBarActions
import com.wedgess.luas.presentation.main.model.TopAppBarState
import com.wedgess.luas.presentation.navigation.Screens

fun NavGraphBuilder.forecastRoot(
    onUpdateAppbarState: (TopAppBarState) -> Unit,
    onRefreshProgressChanged: (Float) -> Unit
) {
    composable<Screens.Forecast> {
        var refreshAction by remember { mutableStateOf({}) }

        LaunchedEffect(Unit) {
            onUpdateAppbarState(
                TopAppBarState(
                    title = "Luas Forecast",
                    actions = { ForecastAppBarActions(onRefresh = refreshAction) },
                    hasProgress = true
                )
            )
        }
        ForecastScreen(
            setRefreshAction = { refreshAction = it },
            onProgressChange = onRefreshProgressChanged
        )
    }
}
