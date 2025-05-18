package com.wedgess.luas.presentation.map.compose.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.wedgess.luas.presentation.map.MapContract
import com.wedgess.luas.presentation.map.model.MapDialogState

@Composable
fun MapDialogs(
    dialogState: MapDialogState,
    onEvent: (MapContract.Event) -> Unit,
) {
    when (dialogState) {
        MapDialogState.None -> Unit
        MapDialogState.LocationPermissionRationale -> AlertDialog(
            onDismissRequest = { onEvent(MapContract.Event.OnDismissDialogClick) },
            title = { Text("Location Permission") },
            text = { Text("Location permission is only required if you wish to show your current location on the map. If not, you can Deny the permission for now and if you wish to allow it in the future, then you can do so from your devices Settings") },
            confirmButton = {
                TextButton(onClick = { onEvent(MapContract.Event.OnAcceptPermissionClick) }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(MapContract.Event.OnDismissDialogClick) }) {
                    Text("Deny")
                }
            },
        )

        MapDialogState.LocationPermissionPermanentlyDenied -> AlertDialog(
            onDismissRequest = { onEvent(MapContract.Event.OnDismissDialogClick) },
            title = { Text("Location Permission") },
            text = { Text("Location permission has been permanently denied, you must manually grant it from your devices Settings. Do you want to grant location permission now?") },
            confirmButton = {
                TextButton(onClick = { onEvent(MapContract.Event.OnOpenAppSettingsPermissionClick)}) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(MapContract.Event.OnIgnoreLocationPermissionClick) }) {
                    Text("No")
                }
            },
        )
    }

}
