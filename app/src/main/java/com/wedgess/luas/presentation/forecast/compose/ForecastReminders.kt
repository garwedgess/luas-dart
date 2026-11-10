package com.wedgess.luas.presentation.forecast.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.presentation.forecast.luastab.compose.components.LuasForecastAlarmRow

/** The station-row control opens reminder details without displacing departure rows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastReminders(alarms: List<Alarm>, onCancel: (String) -> Unit, modifier: Modifier = Modifier) {
    var showReminders by remember(alarms.isEmpty()) { mutableStateOf(false) }
    if (alarms.isNotEmpty()) {
        TextButton(onClick = { showReminders = true }, modifier = modifier) {
            Text(pluralStringResource(R.plurals.active_reminders, alarms.size, alarms.size))
        }
    }
    if (showReminders && alarms.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showReminders = false }) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Text(stringResource(R.string.reminders_title), style = MaterialTheme.typography.titleLarge) }
                items(alarms, key = { it.id }) { alarm -> LuasForecastAlarmRow(alarm, onCancelAlarm = onCancel) }
            }
        }
    }
}
