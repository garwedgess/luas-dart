package com.wedgess.luas.presentation.forecast.luastab.compose.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wedgess.luas.R
import com.wedgess.luas.di.ForecastTabViewModelFactory
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.presentation.components.DropdownTextField
import com.wedgess.luas.presentation.components.EmptyContent
import com.wedgess.luas.presentation.components.ErrorContent
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.forecast.luastab.LuasForecastTabContract
import com.wedgess.luas.presentation.forecast.luastab.viewmodel.LuasForecastTabViewModel
import com.wedgess.luas.presentation.model.Compose
import com.wedgess.luas.presentation.model.DropdownItem
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun LuasForecastTabContent(
    line: LuasLineEntity,
    onRefreshAction: (() -> Unit) -> Unit,
    onProgressChange: (Float) -> Unit,
    luasForecastTabViewModel: LuasForecastTabViewModel = hiltViewModel(
        key = line.name,
        creationCallback = { factory: ForecastTabViewModelFactory ->
            factory.create(line)
        }
    )
) {
    val uiResult by luasForecastTabViewModel.uiResult.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        onRefreshAction { luasForecastTabViewModel.onEvent(LuasForecastTabContract.Event.OnRefresh) }
    }
    val onStopSelect = remember(luasForecastTabViewModel) {
        {
                stop: DropdownItem ->
            luasForecastTabViewModel.onEvent(LuasForecastTabContract.Event.OnStopSelected(stop.text))
        }
    }
    val onShowTravelUpdatesDialog = remember(luasForecastTabViewModel) {
        {
            luasForecastTabViewModel.onEvent(LuasForecastTabContract.Event.OnShowTravelUpdatesDialog)
        }
    }
    val onTramClick = remember(luasForecastTabViewModel) {
        {
                mins: Int, destination: String ->
            luasForecastTabViewModel.onEvent(LuasForecastTabContract.Event.OnShowNotificationsDialog(mins, destination))
        }
    }
    val onCancelAlarm = remember(luasForecastTabViewModel) {
        {
            luasForecastTabViewModel.onEvent(LuasForecastTabContract.Event.OnStopNotification)
        }
    }
    uiResult.Compose(
        onLoading = {
            LoadingContent("Loading forecast")
        },
        onError = {
            ErrorContent("Failed to load forecast", subTitle = it)
        },
        onEmpty = {
            EmptyContent(it)
        },
        onSuccess = { uiState ->
            LuasTabListContent(
                uiState = uiState,
                onStopSelect = onStopSelect,
                onShowTravelUpdatesDialog = onShowTravelUpdatesDialog,
                onTramClick = onTramClick,
                onCancelAlarm = onCancelAlarm,
                onProgressChange = onProgressChange
            )
            LuasForecastTabDialogs(
                dialogsState = uiState.dialog,
                luasNotificationState = uiState.luasNotificationState,
                onEvent = luasForecastTabViewModel::onEvent
            )
        }
    )
}

@SuppressLint("ComposeModifierMissing")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LuasTabListContent(
    uiState: LuasForecastTabContract.UiState,
    onShowTravelUpdatesDialog: () -> Unit,
    onStopSelect: (DropdownItem) -> Unit,
    onTramClick: (Int, String) -> Unit,
    onProgressChange: (Float) -> Unit,
    onCancelAlarm: () -> Unit
) {
    LaunchedEffect(uiState.refreshProgress) {
        onProgressChange(uiState.refreshProgress)
    }

    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        stickyHeader {
            DropdownTextField(
                modifier = Modifier.fillMaxWidth(),
                label = "Stop",
                valueFormatter = { item -> item.text },
                options = uiState.dropdownOptions,
                selectedValue = uiState.selectedStop,
                onValueChange = onStopSelect
            )
        }
        item {
            AnimatedVisibility(uiState.alarmIsRunning) {
                LuasForecastAlarmRow(
                    luasNotificationState = uiState.luasNotificationState,
                    onCancelAlarm = onCancelAlarm
                )
            }
        }
        item {
            LuasForecastStatusMessage(
                message = uiState.forecast.message,
                showTravelUpdatesDialog = onShowTravelUpdatesDialog
            )
        }
        item {
            Column {
                LuasDirectionHeader(
                    title = stringResource(R.string.forecast_title_outbound),
                    noTramsDue = uiState.forecast.outboundTrams.isEmpty()
                )
                AnimatedVisibility(visible = uiState.forecast.outboundTrams.isNotEmpty()) {
                    LuasForecastHeader()
                }
            }
        }
        items(uiState.forecast.outboundTrams) { tram ->
            LuasForecastItemRow(
                dueIn = tram.dueMins,
                destination = tram.destination,
                onRowClick = onTramClick
            )
        }

        item {
            Column {
                HorizontalDivider()
                LuasDirectionHeader(
                    title = stringResource(R.string.forecast_title_inbound),
                    noTramsDue = uiState.forecast.inboundTrams.isEmpty()
                )
                AnimatedVisibility(visible = uiState.forecast.inboundTrams.isNotEmpty()) {
                    LuasForecastHeader()
                }
            }
        }
        items(uiState.forecast.inboundTrams) { tram ->
            LuasForecastItemRow(
                dueIn = tram.dueMins,
                destination = tram.destination,
                onRowClick = onTramClick
            )
        }
    }
}

@Preview
@Composable
private fun ForecastTabContentPreview() {
    LuasTheme {
        Surface {
            LuasForecastTabContent(LuasLineEntity.GREEN, onRefreshAction = {}, onProgressChange = {})
        }
    }
}
