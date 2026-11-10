package com.wedgess.luas.presentation.forecast.luastab.compose.dialogs

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wedgess.luas.R
import com.wedgess.luas.presentation.components.ListPicker
import com.wedgess.luas.ui.theme.LuasTheme
import kotlinx.collections.immutable.toImmutableList

@SuppressLint("ComposeModifierMissing")
@Composable
fun NotificationTimeDialog(
    currentMinutes: Int,
    dueInMinutes: Int,
    onMinutesChange: (Int) -> Unit,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    exactAlarmsAllowed: Boolean = true,
    notificationsAllowed: Boolean = true,
    onOpenSettings: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.alarm_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    pluralStringResource(
                        R.plurals.picker_minutes,
                        currentMinutes,
                        currentMinutes
                    )
                )
                ListPicker(
                    initialValue = currentMinutes,
                    values = (1..dueInMinutes).map { it }.toImmutableList(),
                    onValueChange = onMinutesChange,
                    onIsErrorChange = {},
                    enableEdition = true
                )

                // Degraded behaviour is explained here once, when the user is already deciding; it never prompts.
                if (!notificationsAllowed) {
                    Text(
                        text = stringResource(R.string.alarm_note_notifications_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (!exactAlarmsAllowed) {
                    Text(
                        text = stringResource(R.string.alarm_note_inexact),
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.btn_open_settings))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.alarm_cancel))
                    }
                    Button(onClick = { onConfirm(currentMinutes) }) {
                        Text(stringResource(R.string.alarm_confirm))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun NotificationTimeDialogPreview() {
    LuasTheme {
        NotificationTimeDialog(
            currentMinutes = 4,
            dueInMinutes = 10,
            onDismiss = {},
            onMinutesChange = {},
            onConfirm = {}
        )
    }
}
