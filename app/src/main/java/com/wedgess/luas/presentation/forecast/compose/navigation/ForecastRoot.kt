package com.wedgess.luas.presentation.forecast.compose.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.wedgess.luas.presentation.forecast.compose.ForecastScreen
import com.wedgess.luas.presentation.navigation.Screens

fun NavGraphBuilder.forecastRoot() {
    composable<Screens.Forecast> {
        ForecastScreen()
    }
}