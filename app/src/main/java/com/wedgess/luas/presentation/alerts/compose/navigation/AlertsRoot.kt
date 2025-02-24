package com.wedgess.luas.presentation.alerts.compose.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.wedgess.luas.R
import com.wedgess.luas.presentation.alerts.compose.TravelUpdatesWebViewContent
import com.wedgess.luas.presentation.main.model.TopAppBarState
import com.wedgess.luas.presentation.navigation.Screens

fun NavGraphBuilder.alertsRoot(
    onUpdateAppbarState: (TopAppBarState) -> Unit
) {
    composable<Screens.Alerts> {
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            onUpdateAppbarState(
                TopAppBarState(title = context.getString(R.string.nav_title_alerts))
            )
        }
        TravelUpdatesWebViewContent()
    }
}
