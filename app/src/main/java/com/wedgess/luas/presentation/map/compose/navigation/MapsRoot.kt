package com.wedgess.luas.presentation.map.compose.navigation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.wedgess.luas.R
import com.wedgess.luas.presentation.base.CollectSideEffect
import com.wedgess.luas.presentation.main.model.TopAppBarState
import com.wedgess.luas.presentation.map.MapContract
import com.wedgess.luas.presentation.map.compose.MapScreen
import com.wedgess.luas.presentation.map.viewmodel.MapViewModel
import com.wedgess.luas.presentation.navigation.Screens

@OptIn(ExperimentalPermissionsApi::class)
fun NavGraphBuilder.mapRoot(onUpdateAppbarState: (TopAppBarState) -> Unit) {
    composable<Screens.Map> {
        val viewModel: MapViewModel = hiltViewModel()
        val uiResult by viewModel.uiResult.collectAsStateWithLifecycle()
        val permissionsState = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            onUpdateAppbarState(
                TopAppBarState(title = context.getString(R.string.nav_title_map))
            )
        }

        LaunchedEffect(permissionsState.permissions.map { it.status }) {
            viewModel.onEvent(MapContract.Event.OnPermissionStateChanged(permissionsState))

            if (!permissionsState.allPermissionsGranted &&
                !permissionsState.permissions.any { it.status.shouldShowRationale }
            ) {
                viewModel.onEvent(MapContract.Event.OnLocationWasRequested)
                permissionsState.launchMultiplePermissionRequest()
            }
        }

        CollectSideEffect(sideEffect = viewModel.sideEffect) {
            if (it == MapContract.Effect.ShowSystemLocationPermissionDialog) {
                viewModel.onEvent(MapContract.Event.OnLocationWasRequested)
                permissionsState.launchMultiplePermissionRequest()
            } else if (it == MapContract.Effect.OpenAppPermissionScreen) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }

        MapScreen(uiResult = uiResult, onEvent = viewModel::onEvent)
    }
}
