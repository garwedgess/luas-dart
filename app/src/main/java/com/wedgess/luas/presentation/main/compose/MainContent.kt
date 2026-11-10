package com.wedgess.luas.presentation.main.compose

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wedgess.luas.presentation.components.MainTopAppbar
import com.wedgess.luas.presentation.main.MainContract
import com.wedgess.luas.presentation.main.MainViewModel
import com.wedgess.luas.presentation.navigation.BottomNavigationBar
import com.wedgess.luas.presentation.navigation.MainNavigationGraph
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val navHostController = rememberNavController()
    val backStackEntry = navHostController.currentBackStackEntryAsState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LuasTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                MainTopAppbar(
                    topAppBarState = uiState.appBarState,
                    onTransportTypeChange = {
                        viewModel.onEvent(MainContract.Event.OnTransportTypeChange(it))
                    }
                )
            },
            bottomBar = {
                BottomNavigationBar(
                    selectedTransportType = uiState.transportType,
                    selectedItemRoute = backStackEntry.value?.destination?.route,
                    onNavigateTo = { route ->
                        navHostController.navigate(route) {
                            popUpTo(navHostController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        ) { innerPadding ->
            MainNavigationGraph(
                navController = navHostController,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .fillMaxSize(),
                onUpdateAppbarState = {
                    viewModel.onEvent(MainContract.Event.OnUpdateAppBarState(it))
                }
            )
        }
    }
}
