package com.wedgess.luas.presentation.forecast.luastab.compose.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.wedgess.luas.presentation.components.EmptyContent
import com.wedgess.luas.presentation.components.ErrorContent
import com.wedgess.luas.presentation.components.FreshnessBadge
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.components.NextDepartureHeader
import com.wedgess.luas.presentation.components.OfflineContent
import com.wedgess.luas.presentation.components.StaleDataBanner
import com.wedgess.luas.presentation.components.stationpicker.StationPicker
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
    val onToggleFavourite = remember(luasForecastTabViewModel) {
        {
                name: String, favourite: Boolean ->
            luasForecastTabViewModel.onEvent(LuasForecastTabContract.Event.OnToggleFavourite(name, favourite))
        }
    }
    val onShowTravelUpdatesDialog = remember(luasForecastTabViewModel) {
        {
            luasForecastTabViewModel.onEvent(LuasForecastTabContract.Event.OnShowTravelUpdatesDialog)
        }
    }
    val onTramClick = remember(luasForecastTabViewModel) {
        {
                mins: Int, destination: String, direction: DepartureDirection ->
            luasForecastTabViewModel.onEvent(
                LuasForecastTabContract.Event.OnShowNotificationsDialog(mins, destination, direction)
            )
        }
    }
    val onCancelAlarm = remember(luasForecastTabViewModel) {
        {
                alarmId: String ->
            luasForecastTabViewModel.onEvent(LuasForecastTabContract.Event.OnStopNotification(alarmId))
        }
    }
    uiResult.Compose(
        onLoading = {
            LoadingContent("Loading forecast")
        },
        onError = {
            ErrorContent("Failed to load forecast", subTitle = it)
        },
        onOffline = {
            OfflineContent(onRetry = { luasForecastTabViewModel.onEvent(LuasForecastTabContract.Event.OnRefresh) })
        },
        onEmpty = {
            EmptyContent(it)
        },
        onSuccess = { uiState ->
            LuasTabListContent(
                uiState = uiState,
                onStopSelect = onStopSelect,
                onToggleFavourite = onToggleFavourite,
                onShowTravelUpdatesDialog = onShowTravelUpdatesDialog,
                onTramClick = onTramClick,
                onCancelAlarm = onCancelAlarm
            )
            LuasForecastTabDialogs(
                dialogsState = uiState.dialog,
                luasNotificationState = uiState.luasNotificationState,
                alarmCapabilities = uiState.alarmCapabilities,
                onEvent = luasForecastTabViewModel::onEvent
            )
        }
    )
}

@Suppress("LongParameterList") // Stateless UI: one callback per user action.
@SuppressLint("ComposeModifierMissing")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LuasTabListContent(
    uiState: LuasForecastTabContract.UiState,
    onShowTravelUpdatesDialog: () -> Unit,
    onStopSelect: (DropdownItem) -> Unit,
    onToggleFavourite: (String, Boolean) -> Unit,
    onTramClick: (Int, String, DepartureDirection) -> Unit,
    onCancelAlarm: (String) -> Unit
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
            onToggleFavourite = onToggleFavourite,
            onTramClick = onTramClick,
            onCancelAlarm = onCancelAlarm
        )
    }
}

@Suppress("LongParameterList") // Stateless UI: one callback per user action.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LuasTramList(
    uiState: LuasForecastTabContract.UiState,
    onShowTravelUpdatesDialog: () -> Unit,
    onStopSelect: (DropdownItem) -> Unit,
    onToggleFavourite: (String, Boolean) -> Unit,
    onTramClick: (Int, String, DepartureDirection) -> Unit,
    onCancelAlarm: (String) -> Unit
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
            StationPicker(
                label = "Stop",
                selected = uiState.selectedStop,
                options = uiState.dropdownOptions,
                favourites = uiState.stationPreferences.favourites,
                recents = uiState.stationPreferences.recents,
                onSelect = onStopSelect,
                onToggleFavourite = onToggleFavourite
            )
        }
        if (uiState.isStale) {
            item { StaleDataBanner(lastUpdated = uiState.forecast.fetchedAt) }
        } else {
            item { FreshnessBadge(fetchedAt = uiState.forecast.fetchedAt) }
        }
        items(uiState.activeAlarms, key = { it.id }) { alarm ->
            LuasForecastAlarmRow(alarm = alarm, onCancelAlarm = onCancelAlarm)
        }
        uiState.forecast.items.filter { it.dueMins != null && it.expectedAt != null }
            .minByOrNull { requireNotNull(it.dueMins) }?.let { next ->
                item {
                    NextDepartureHeader(
                        destination = next.destination,
                        dueMins = requireNotNull(next.dueMins),
                        expectedTime = requireNotNull(next.expectedAt).toClockTime(),
                        stale = uiState.isStale
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
                onRowClick = { mins, destination -> onTramClick(mins, destination, tram.direction) }
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
                onRowClick = { mins, destination -> onTramClick(mins, destination, tram.direction) }
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
