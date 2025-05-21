package com.wedgess.luas.presentation.news.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.wedgess.luas.R
import com.wedgess.luas.presentation.main.model.TopAppBarState
import com.wedgess.luas.presentation.navigation.Screens
import com.wedgess.luas.presentation.news.compose.TravelUpdatesWebViewContent

fun NavGraphBuilder.newsRoot(
    onUpdateAppbarState: (TopAppBarState) -> Unit
) {
    composable<Screens.Alerts> {
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            onUpdateAppbarState(
                TopAppBarState(title = context.getString(R.string.nav_title_news))
            )
        }
        TravelUpdatesWebViewContent()
    }
}
