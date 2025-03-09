package com.wedgess.luas.presentation.map.compose

import android.Manifest
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.wedgess.luas.presentation.components.EmptyContent
import com.wedgess.luas.presentation.components.ErrorContent
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.map.MapContract
import com.wedgess.luas.presentation.map.compose.components.MapLibreMap
import com.wedgess.luas.presentation.model.Compose
import com.wedgess.luas.presentation.model.UiResult

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(uiResult: UiResult<MapContract.UiState>) {
    uiResult.Compose(
        onLoading = { LoadingContent(title = "Loading map info...") },
        onError = { ErrorContent(title = "Failed to load map info: $it") },
        onEmpty = { EmptyContent(title = "No map info...") },
        onSuccess = {
            val locationPermissionState =
                rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
            if (locationPermissionState.status.isGranted) {
                MapLibreMap(it)
            } else {
                Text("No permission...")
            }
        }
    )
}
