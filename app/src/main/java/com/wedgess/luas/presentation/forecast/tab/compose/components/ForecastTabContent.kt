package com.wedgess.luas.presentation.forecast.tab.compose.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wedgess.luas.R
import com.wedgess.luas.di.ForecastTabViewModelFactory
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.presentation.components.DropdownTextField
import com.wedgess.luas.presentation.components.EmptyContent
import com.wedgess.luas.presentation.components.ErrorContent
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.forecast.tab.ForecastTabContract
import com.wedgess.luas.presentation.forecast.tab.viewmodel.ForecastTabViewModel
import com.wedgess.luas.presentation.model.Compose
import com.wedgess.luas.ui.theme.LuasTheme
import kotlinx.collections.immutable.toImmutableList

@Composable
fun ForecastTabContent(
    line: LuasLineEntity,
    onRefreshAction: (() -> Unit) -> Unit,
    onProgressChange: (Float) -> Unit,
    forecastTabViewModel: ForecastTabViewModel = hiltViewModel(
        key = line.name,
        creationCallback = { factory: ForecastTabViewModelFactory ->
            factory.create(line)
        },
    ),
) {
    val uiResult by forecastTabViewModel.uiResult.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        onRefreshAction { forecastTabViewModel.onEvent(ForecastTabContract.Event.OnRefresh) }
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
            TabListContent(
                uiState = uiState,
                onStopSelected = { stop -> forecastTabViewModel.onEvent((ForecastTabContract.Event.OnStopSelected(stop))) },
                onProgressChange = onProgressChange,
                onShowTravelUpdatesDialog = { forecastTabViewModel.onEvent(ForecastTabContract.Event.OnShowTravelUpdatesDialog) },
                onTramClick = { mins, destination ->
                    forecastTabViewModel.onEvent(
                        ForecastTabContract.Event.OnShowNotificationsDialog(
                            mins,
                            destination,
                        ),
                    )
                },
                onCancelAlarm = {
                    forecastTabViewModel.onEvent(ForecastTabContract.Event.OnStopNotification)
                },
            )
            ForecastTabDialogs(
                dialogsState = uiState.dialog,
                notificationState = uiState.notificationState,
                onEvent = forecastTabViewModel::onEvent,
            )
        },
    )
}

@SuppressLint("ComposeModifierMissing")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabListContent(
    uiState: ForecastTabContract.UiState,
    onShowTravelUpdatesDialog: () -> Unit,
    onStopSelected: (StopEntity) -> Unit,
    onTramClick: (Int, String) -> Unit,
    onProgressChange: (Float) -> Unit,
    onCancelAlarm: () -> Unit,
) {
    LaunchedEffect(uiState.refreshProgress) {
        onProgressChange(uiState.refreshProgress)
    }

    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        stickyHeader {
            DropdownTextField(
                modifier = Modifier.fillMaxWidth(),
                label = "Stop",
                valueFormatter = { item -> item.name },
                options = uiState.stops.toImmutableList(),
                selectedValue = uiState.selectedStop,
                onValueChange = onStopSelected,
            )
        }
        item {
            AnimatedVisibility(uiState.alarmIsRunning) {
                ForecastAlarmRow(uiState.notificationState, onCancelAlarm)
            }
        }
        item {
            ForecastStatusMessage(
                message = uiState.forecast.message,
                showTravelUpdatesDialog = onShowTravelUpdatesDialog,
            )
        }
        item {
            TramDirectionHeader(
                title = stringResource(R.string.forecast_title_outbound),
                noTramsDue = uiState.forecast.outboundTrams.isEmpty(),
            )
        }
        items(uiState.forecast.outboundTrams) { tram ->
            ForecastItemRow(
                dueInMins = tram.dueMins,
                destination = tram.destination,
                onRowClick = onTramClick,
            )
        }

        item {
            TramDirectionHeader(
                title = stringResource(R.string.forecast_title_inbound),
                noTramsDue = uiState.forecast.inboundTrams.isEmpty(),
            )
        }
        items(uiState.forecast.inboundTrams) { tram ->
            ForecastItemRow(
                dueInMins = tram.dueMins,
                destination = tram.destination,
                onRowClick = onTramClick,
            )
        }
    }
}

@Preview
@Composable
private fun ForecastTabContentPreview() {
    LuasTheme {
        Surface {
            ForecastTabContent(LuasLineEntity.GREEN, onRefreshAction = {}, onProgressChange = {})
        }
    }
}
