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
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wedgess.luas.presentation.components.MainTopAppbar
import com.wedgess.luas.presentation.forecast.luastab.compose.components.RefreshProgressIndicator
import com.wedgess.luas.presentation.main.MainContract
import com.wedgess.luas.presentation.main.MainViewModel
import com.wedgess.luas.presentation.navigation.BottomNavigationBar
import com.wedgess.luas.presentation.navigation.MainNavigationGraph
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val navHostController = rememberNavController()
    val backStackEntry = navHostController.currentBackStackEntryAsState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LuasTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                Column(Modifier.wrapContentHeight()) {
                    MainTopAppbar(
                        topAppBarState = uiState.appBarState,
                        onTransportTypeChange = {
                            viewModel.onEvent(MainContract.Event.OnTransportTypeChange(it))
                        },
                    )
                    AnimatedVisibility(visible = uiState.appBarState.hasProgress) {
                        RefreshProgressIndicator(uiState.refreshProgress)
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
                        },
                    )
                }
            },
        ) { innerPadding ->
            MainNavigationGraph(
                navController = navHostController,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                onUpdateAppbarState = {
                    viewModel.onEvent(MainContract.Event.OnUpdateAppBarState(it))
                },
                onRefreshProgressChanged = {
                    viewModel.onEvent(MainContract.Event.OnUpdateRefreshProgress(it))
                },
            )
        }
    }
}
