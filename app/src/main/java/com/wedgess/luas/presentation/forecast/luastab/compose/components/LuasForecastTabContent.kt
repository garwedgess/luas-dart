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
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.presentation.components.DropdownTextField
import com.wedgess.luas.presentation.components.EmptyContent
import com.wedgess.luas.presentation.components.ErrorContent
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.components.StaleDataBanner
import com.wedgess.luas.presentation.extensions.toClockTime
import com.wedgess.luas.presentation.forecast.compose.RefreshProgressIndicator
import com.wedgess.luas.presentation.forecast.luastab.LuasForecastTabContract
import com.wedgess.luas.presentation.forecast.luastab.viewmodel.LuasForecastTabViewModel
import com.wedgess.luas.presentation.model.Compose
import com.wedgess.luas.presentation.model.DropdownItem
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun LuasForecastTabContent(
    line: LuasLineEntity,
    onRefreshAction: (() -> Unit) -> Unit,
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
                onCancelAlarm = onCancelAlarm
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
    onCancelAlarm: () -> Unit
) {
    Column {
        RefreshProgressIndicator(
            cycleKey = uiState.forecast.fetchedAt,
            intervalMillis = LuasForecastTabContract.REFRESH_INTERVAL_MILLIS
        )
        LuasTramList(
            uiState = uiState,
            onShowTravelUpdatesDialog = onShowTravelUpdatesDialog,
            onStopSelect = onStopSelect,
            onTramClick = onTramClick,
            onCancelAlarm = onCancelAlarm
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LuasTramList(
    uiState: LuasForecastTabContract.UiState,
    onShowTravelUpdatesDialog: () -> Unit,
    onStopSelect: (DropdownItem) -> Unit,
    onTramClick: (Int, String) -> Unit,
    onCancelAlarm: () -> Unit
) {
    val outbound = uiState.forecast.byDirection(DepartureDirection.OUTBOUND)
    val inbound = uiState.forecast.byDirection(DepartureDirection.INBOUND)
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
        if (uiState.isStale) {
            item { StaleDataBanner(lastUpdated = uiState.forecast.fetchedAt) }
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
                    noTramsDue = outbound.isEmpty()
                )
                AnimatedVisibility(visible = outbound.isNotEmpty()) {
                    LuasForecastHeader()
                }
            }
        }
        items(outbound) { tram ->
            LuasForecastItemRow(
                dueIn = tram.dueMins ?: -1,
                destination = tram.destination,
                stale = uiState.isStale,
                expectedTime = tram.expectedAt?.toClockTime(),
                onRowClick = onTramClick
            )
        }

        item {
            Column {
                HorizontalDivider()
                LuasDirectionHeader(
                    title = stringResource(R.string.forecast_title_inbound),
                    noTramsDue = inbound.isEmpty()
                )
                AnimatedVisibility(visible = inbound.isNotEmpty()) {
                    LuasForecastHeader()
                }
            }
        }
        items(inbound) { tram ->
            LuasForecastItemRow(
                dueIn = tram.dueMins ?: -1,
                destination = tram.destination,
                stale = uiState.isStale,
                expectedTime = tram.expectedAt?.toClockTime(),
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
            LuasForecastTabContent(LuasLineEntity.GREEN, onRefreshAction = {})
        }
    }
}
