package com.wedgess.luas.presentation.map.compose.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.wedgess.luas.R
import com.wedgess.luas.presentation.main.model.TopAppBarState
import com.wedgess.luas.presentation.map.compose.MapScreen
import com.wedgess.luas.presentation.map.compose.components.RequestLocationPermissions
import com.wedgess.luas.presentation.map.viewmodel.MapViewModel
import com.wedgess.luas.presentation.navigation.Screens

fun NavGraphBuilder.mapRoot(onUpdateAppbarState: (TopAppBarState) -> Unit) {
    composable<Screens.Map> {
        val viewModel: MapViewModel = hiltViewModel()
        val uiResult by viewModel.uiResult.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            onUpdateAppbarState(
                TopAppBarState(title = context.getString(R.string.nav_title_map))
            )
        }

        RequestLocationPermissions(
            onPermissionGranted = {},
            onPermissionDenied = {},
            onPermissionsRevoked = {},
            onShowRationale = {}
        )

        MapScreen(uiResult)
    }
}
