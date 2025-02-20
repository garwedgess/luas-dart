package com.wedgess.luas.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.wedgess.luas.presentation.forecast.compose.navigation.forecastRoot
import com.wedgess.luas.presentation.main.model.TopAppBarState
import com.wedgess.luas.presentation.map.compose.navigation.mapRoot

@Composable
fun MainNavigationGraph(
    navController: NavHostController,
    onUpdateAppbarState: (TopAppBarState) -> Unit,
    onRefreshProgressChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screens.Forecast,
        modifier = modifier
    ) {
        forecastRoot(onUpdateAppbarState, onRefreshProgressChanged)
        mapRoot(onUpdateAppbarState)
    }
}