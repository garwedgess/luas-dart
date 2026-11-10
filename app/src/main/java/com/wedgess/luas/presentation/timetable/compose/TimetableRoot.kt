package com.wedgess.luas.presentation.timetable.compose

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.wedgess.luas.R
import com.wedgess.luas.presentation.main.model.TopAppBarState
import com.wedgess.luas.presentation.navigation.Screens
import com.wedgess.luas.presentation.timetable.TimetableViewModel

fun NavGraphBuilder.timetableRoot(onUpdateAppbarState: (TopAppBarState) -> Unit) {
    composable<Screens.Timetable> {
        val viewModel: TimetableViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            onUpdateAppbarState(TopAppBarState(title = context.getString(R.string.nav_title_timetable)))
        }
        LaunchedEffect(state.transport) { viewModel.update(force = false) }
        TimetableScreen(
            state = state,
            onUpdate = { viewModel.update() },
            onStationSelect = viewModel::selectStation,
            onDateSelect = viewModel::selectDate
        )
    }
}
