package com.wedgess.luas.presentation.forecast.compose.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.wedgess.luas.R
import com.wedgess.luas.presentation.base.CollectSideEffect
import com.wedgess.luas.presentation.forecast.ForecastContract
import com.wedgess.luas.presentation.forecast.compose.ForecastScreen
import com.wedgess.luas.presentation.forecast.compose.actions.ForecastAppBarActions
import com.wedgess.luas.presentation.forecast.compose.dialogs.ForecastDialogs
import com.wedgess.luas.presentation.forecast.viewmodel.ForecastViewModel
import com.wedgess.luas.presentation.main.model.TopAppBarState
import com.wedgess.luas.presentation.navigation.Screens

@SuppressLint("InlinedApi")
@OptIn(ExperimentalPermissionsApi::class)
fun NavGraphBuilder.forecastRoot(
    onUpdateAppbarState: (TopAppBarState) -> Unit,
    onRefreshProgressChanged: (Float) -> Unit
) {
    composable<Screens.Forecast> {
        val viewModel: ForecastViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        var refreshAction by remember { mutableStateOf({}) }
        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

        LaunchedEffect(Unit) {
            onUpdateAppbarState(
                TopAppBarState(
                    title = context.getString(R.string.nav_title_forecast),
                    actions = { ForecastAppBarActions(onRefresh = refreshAction) },
                    hasProgress = true
                )
            )
        }
        LaunchedEffect(permissionState.status) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                viewModel.onEvent(ForecastContract.Event.OnPermissionStateChanged(permissionState))

                if (!permissionState.status.isGranted && !permissionState.status.shouldShowRationale) {
                    viewModel.onEvent(ForecastContract.Event.OnNotificationWasRequested)
                    permissionState.launchPermissionRequest()
                }
            }
        }
        CollectSideEffect(sideEffect = viewModel.sideEffect) {
            if (it == ForecastContract.Effect.ShowSystemNotificationPermissionDialog) {
                viewModel.onEvent(ForecastContract.Event.OnNotificationWasRequested)
                permissionState.launchPermissionRequest()
            } else if (it == ForecastContract.Effect.OpenAppPermissionScreen) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }
        ForecastScreen(
            setRefreshAction = { refreshAction = it },
            onProgressChange = onRefreshProgressChanged
        )
        ForecastDialogs(uiState.dialog, viewModel::onEvent)
    }
}
