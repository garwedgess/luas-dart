package com.wedgess.luas.presentation.timetable.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
        var info by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            onUpdateAppbarState(
                TopAppBarState(
                    title = context.getString(R.string.nav_title_timetable),
                    actions = {
                        IconButton(onClick = { info = true }) {
                            Icon(Icons.Outlined.Info, stringResource(R.string.timetable_info))
                        }
                    }
                )
            )
        }
        LaunchedEffect(state.transport) { viewModel.update(force = false) }
        if (info) TimetableInfoSheet(state, { viewModel.update() }, { info = false })
        Surface {
            TimetableScreen(
                state = state,
                showInfoAction = false,
                onUpdate = { viewModel.update() },
                onStationSelect = viewModel::selectStation,
                onToggleFavourite = viewModel::toggleFavourite,
                onDateSelect = viewModel::selectDate
            )
        }
    }
}
