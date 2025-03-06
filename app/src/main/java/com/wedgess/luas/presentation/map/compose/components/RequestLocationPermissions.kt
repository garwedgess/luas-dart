package com.wedgess.luas.presentation.map.compose.components

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Composable function to request location permissions and handle different scenarios.
 *
 * @param onPermissionGranted Callback to be executed when all requested permissions are granted.
 * @param onPermissionDenied Callback to be executed when any requested permission is denied.
 * @param onPermissionsRevoked Callback to be executed when previously granted permissions are revoked.
 * @param onShowRationale Callback to be executed when any requested permission should show rationale.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestLocationPermissions(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionsRevoked: () -> Unit,
    onShowRationale: () -> Unit
) {
    val permissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )

    LaunchedEffect(key1 = permissionState.permissions.map { it.status }) {
        when {
            permissionState.allPermissionsGranted -> {
                onPermissionGranted()
            }

            permissionState.permissions.any { it.status.shouldShowRationale } -> {
                onShowRationale()
            }

            permissionState.permissions.any { !it.status.isGranted } -> {
                permissionState.launchMultiplePermissionRequest()
            }

            permissionState.permissions.all { !it.status.isGranted } -> {
                onPermissionsRevoked()
            }

            else -> {
                onPermissionDenied()
            }
        }
    }
}
