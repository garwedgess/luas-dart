package com.wedgess.luas.presentation.main.compose

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wedgess.luas.R
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.components.MainTopAppbar
import com.wedgess.luas.presentation.main.MainContract
import com.wedgess.luas.presentation.main.MainViewModel
import com.wedgess.luas.presentation.navigation.BottomNavigationBar
import com.wedgess.luas.presentation.navigation.MainNavigationGraph
import com.wedgess.luas.presentation.navigation.Screens
import com.wedgess.luas.presentation.navigation.reminder.ReminderNavigationError
import com.wedgess.luas.presentation.navigation.reminder.ReminderNavigationViewModel
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    reminderNavigation: ReminderNavigationViewModel = hiltViewModel()
) {
    val navHostController = rememberNavController()
    val backStackEntry = navHostController.currentBackStackEntryAsState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reminderState by reminderNavigation.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val reminderError = stringResource(
        if (reminderState.error == ReminderNavigationError.DEPARTURE_UNAVAILABLE) {
            R.string.reminder_departure_unavailable
        } else {
            R.string.reminder_open_failed
        }
    )
    LaunchedEffect(reminderState.error) {
        if (reminderState.error != null) {
            snackbar.showSnackbar(reminderError)
            reminderNavigation.clearError()
        }
    }
    LaunchedEffect(reminderState.target?.token, uiState.transportType) {
        val target = reminderState.target ?: return@LaunchedEffect
        if (uiState.transportType == target.alarm.transport) {
            navHostController.navigate(Screens.Forecast) {
                popUpTo(navHostController.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(uiState.transportType, backStackEntry.value?.destination) {
        if (uiState.transportType == TransportType.DART &&
            backStackEntry.value?.destination?.hasRoute<Screens.Alerts>() == true
        ) {
            navHostController.navigate(Screens.Forecast) {
                popUpTo(navHostController.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }

    LuasTheme {
        if (reminderState.loading) {
            AlertDialog(
                onDismissRequest = reminderNavigation::cancel,
                title = { Text(stringResource(R.string.reminder_opening)) },
                confirmButton = {
                    TextButton(onClick = reminderNavigation::cancel) { Text(stringResource(R.string.alarm_cancel)) }
                }
            )
        }
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                MainTopAppbar(
                    topAppBarState = uiState.appBarState,
                    onTransportTypeChange = {
                        reminderNavigation.cancel()
                        viewModel.onEvent(MainContract.Event.OnTransportTypeChange(it))
                    }
                )
            },
            bottomBar = {
                BottomNavigationBar(
                    selectedTransportType = uiState.transportType,
                    selectedItemRoute = backStackEntry.value?.destination?.route,
                    onNavigateTo = { route ->
                        reminderNavigation.cancel()
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
                reminderTarget = reminderState.target,
                onReminderComplete = reminderNavigation::complete,
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
