package com.wedgess.luas.presentation.forecast.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wedgess.luas.R

@Composable
fun DepartureReminderIcon(reminderSet: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = if (reminderSet) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone,
        contentDescription = stringResource(
            if (reminderSet) R.string.forecast_reminder_set else R.string.forecast_set_reminder
        ),
        tint = if (reminderSet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}
