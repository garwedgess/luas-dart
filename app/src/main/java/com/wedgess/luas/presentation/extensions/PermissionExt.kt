package com.wedgess.luas.presentation.extensions

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.wedgess.luas.presentation.model.Permission

@OptIn(ExperimentalPermissionsApi::class)
fun MultiplePermissionsState.toPermission(wasPreviouslyRequested: Boolean) = when {
    this.allPermissionsGranted -> Permission.Granted
    this.permissions.any { !it.status.isGranted && it.status.shouldShowRationale } -> Permission.ShowRationale
    this.permissions.any {
        !it.status.isGranted &&
            !it.status.shouldShowRationale &&
            wasPreviouslyRequested
    } -> Permission.PermanentlyDenied

    this.permissions.all { !it.status.isGranted } -> Permission.Denied
    else -> Permission.Unknown
}

@OptIn(ExperimentalPermissionsApi::class)
fun PermissionState.toPermission(wasPreviouslyRequested: Boolean) = when {
    this.status.isGranted -> Permission.Granted
    this.status.shouldShowRationale -> Permission.ShowRationale
    !this.status.isGranted && wasPreviouslyRequested -> Permission.PermanentlyDenied
    !this.status.isGranted -> Permission.Denied
    else -> Permission.Unknown
}
