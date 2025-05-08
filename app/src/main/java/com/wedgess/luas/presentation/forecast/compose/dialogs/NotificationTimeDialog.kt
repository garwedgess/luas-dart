package com.wedgess.luas.presentation.forecast.compose.dialogs

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
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
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    pluralStringResource(
                        R.plurals.picker_minutes,
                        currentMinutes,
                        currentMinutes,
                    ),
                )
                ListPicker(
                    initialValue = currentMinutes,
                    values = (1..dueInMinutes).map { it }.toImmutableList(),
                    onValueChange = onMinutesChange,
                    onIsErrorChange = {},
                    enableEdition = true,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(onClick = { onConfirm(currentMinutes) }) {
                        Text("Confirm")
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
            onConfirm = {},
        )
    }
}
