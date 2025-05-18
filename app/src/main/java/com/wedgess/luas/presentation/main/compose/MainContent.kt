package com.wedgess.luas.presentation.main.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wedgess.luas.presentation.components.MainTopAppbar
import com.wedgess.luas.presentation.forecast.tab.compose.components.RefreshProgressIndicator
import com.wedgess.luas.presentation.main.model.TopAppBarState
import com.wedgess.luas.presentation.navigation.BottomNavigationBar
import com.wedgess.luas.presentation.navigation.MainNavigationGraph
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun MainContent() {
    val navHostController = rememberNavController()
    val backStackEntry = navHostController.currentBackStackEntryAsState()

    var topAppBarState by remember {
        mutableStateOf(TopAppBarState(title = "Luas"))
    }
    var refreshProgress by remember { mutableFloatStateOf(0f) }

    LuasTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column(Modifier.wrapContentHeight()) {
                    MainTopAppbar(topAppBarState = topAppBarState)
                    AnimatedVisibility(visible = topAppBarState.hasProgress) {
                        RefreshProgressIndicator(refreshProgress)
                    }
                }
            },
            bottomBar = {
                BottomAppBar {
                    BottomNavigationBar(
                        selectedItemRoute = backStackEntry.value?.destination?.route,
                        onNavigateTo = { route ->
                            if (route != backStackEntry.value?.destination) {
                                navHostController.navigate(route)
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            MainNavigationGraph(
                navController = navHostController,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                onUpdateAppbarState = { newState ->
                    topAppBarState = newState
                },
                onRefreshProgressChanged = {
                    refreshProgress = it
                }
            )
        }
    }
}
