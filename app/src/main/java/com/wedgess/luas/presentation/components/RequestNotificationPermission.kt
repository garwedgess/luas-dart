package com.wedgess.luas.presentation.components

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.getSystemService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("InlinedApi")
@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current
    val activity = context as? Activity
    val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    // Check if we are on Android 13+ before requesting permission
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        }
    }

    when (permissionState.status) {
        is PermissionStatus.Granted -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "LUAS_CHANNEL",
                    "Luas Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Get real-time Luas tram updates."
                }

                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }
        is PermissionStatus.Denied -> {
            if ((permissionState.status as PermissionStatus.Denied).shouldShowRationale) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Notification Permission Required") },
                    text = { Text("This app needs notification permission to send you Luas updates.") },
                    confirmButton = {
                        Button(onClick = { permissionState.launchPermissionRequest() }) {
                            Text("Allow")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { activity?.finish() }) {
                            Text("Exit")
                        }
                    }
                )
            } else {
                Text("Notification permission denied. Enable it in app settings.")
            }
        }
    }
}
