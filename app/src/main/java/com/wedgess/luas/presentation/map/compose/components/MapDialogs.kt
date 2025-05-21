package com.wedgess.luas.presentation.map.compose.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wedgess.luas.R
import com.wedgess.luas.presentation.map.MapContract
import com.wedgess.luas.presentation.map.model.MapDialogState

@Composable
fun MapDialogs(
    dialogState: MapDialogState,
    onEvent: (MapContract.Event) -> Unit
) {
    when (dialogState) {
        MapDialogState.None -> Unit
        MapDialogState.LocationPermissionRationale -> AlertDialog(
            onDismissRequest = { onEvent(MapContract.Event.OnDismissDialogClick) },
            title = { Text(stringResource(R.string.location_permission_dialog_title)) },
            text = { Text(stringResource(R.string.location_permission_rationale_msg)) },
            confirmButton = {
                TextButton(onClick = { onEvent(MapContract.Event.OnAcceptPermissionClick) }) {
                    Text(stringResource(R.string.btn_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(MapContract.Event.OnDismissDialogClick) }) {
                    Text(stringResource(R.string.btn_deny))
                }
            }
        )

        MapDialogState.LocationPermissionPermanentlyDenied -> AlertDialog(
            onDismissRequest = { onEvent(MapContract.Event.OnDismissDialogClick) },
            title = { Text(stringResource(R.string.location_permission_dialog_title)) },
            text = { Text(stringResource(R.string.location_permission_permanently_denied_msg)) },
            confirmButton = {
                TextButton(onClick = { onEvent(MapContract.Event.OnOpenAppSettingsPermissionClick) }) {
                    Text(stringResource(R.string.btn_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(MapContract.Event.OnIgnoreLocationPermissionClick) }) {
                    Text(stringResource(R.string.btn_no))
                }
            }
        )
    }
}
