package com.wedgess.luas.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.wedgess.luas.presentation.model.Permission

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermission(
    permission: String,
    onPermissionResult: (Permission) -> Unit
) {
    val permissionState = rememberPermissionState(permission)

    val updatedOnPermissionResult by rememberUpdatedState(onPermissionResult)

    // Only re-execute when the permission status changes
    DisposableEffect(permissionState.status) {
        val currentStatus = when {
            permissionState.status.isGranted -> Permission.Granted
            permissionState.status.shouldShowRationale -> Permission.ShowRationale
            !permissionState.status.isGranted -> Permission.Denied
            else -> Permission.Unknown
        }

        // Call the callback
        updatedOnPermissionResult(currentStatus)

        // If permission needs to be requested, launch the request
        if (!permissionState.status.isGranted &&
            !permissionState.status.shouldShowRationale) {
            permissionState.launchPermissionRequest()
        }

        onDispose { }
    }
}
