package com.wedgess.luas.presentation.forecast.compose.actions

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.forecast.compose.ForecastRefreshCycle
import com.wedgess.luas.presentation.forecast.compose.ForecastReminders
import com.wedgess.luas.presentation.forecast.viewmodel.ForecastRemindersViewModel
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun ForecastAppBarActions(
    modifier: Modifier = Modifier,
    transportType: TransportType = TransportType.LUAS,
    refreshCycle: ForecastRefreshCycle? = null,
    remindersViewModel: ForecastRemindersViewModel = hiltViewModel(),
    onRefresh: () -> Unit
) {
    val alarms by remindersViewModel.alarms.collectAsStateWithLifecycle()
    Row(modifier) {
        ForecastReminders(alarms.filter { it.transport == transportType }, remindersViewModel::cancel)
        ForecastRefreshButton(refreshCycle, onRefresh)
    }
}

@Preview
@Composable
private fun ForecastAppBarActionsPreview() {
    LuasTheme {
        Surface {
            ForecastRefreshButton(null, {})
        }
    }
}
