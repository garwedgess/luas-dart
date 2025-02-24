package com.wedgess.luas.presentation.forecast.compose.components

import TravelUpdatesDialog
import androidx.compose.runtime.Composable
import com.wedgess.luas.presentation.forecast.model.ForecastDialogState

@Composable
fun ForecastDialogs(dialogsState: ForecastDialogState, onDismissDialog: () -> Unit) {
    when (dialogsState) {
        ForecastDialogState.None -> Unit
        ForecastDialogState.TravelUpdatesAlert -> TravelUpdatesDialog(onDismissDialog)
    }
}
