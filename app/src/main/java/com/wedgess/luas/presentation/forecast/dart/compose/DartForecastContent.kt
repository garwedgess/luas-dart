package com.wedgess.luas.presentation.forecast.dart.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wedgess.luas.presentation.components.EmptyContent
import com.wedgess.luas.presentation.components.ErrorContent
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.components.NextDepartureHeader
import com.wedgess.luas.presentation.components.StaleDataBanner
import com.wedgess.luas.presentation.components.sectionedlist.SectionedList
import com.wedgess.luas.presentation.components.stationpicker.StationPicker
import com.wedgess.luas.presentation.forecast.compose.RefreshProgressIndicator
import com.wedgess.luas.presentation.forecast.dart.DartForecastTabContract
import com.wedgess.luas.presentation.forecast.dart.model.DartForecastSectionRowData
import com.wedgess.luas.presentation.forecast.dart.viewmodel.DartForecastViewModel
import com.wedgess.luas.presentation.forecast.luastab.compose.components.LuasForecastAlarmRow
import com.wedgess.luas.presentation.forecast.luastab.compose.dialogs.NotificationTimeDialog
import com.wedgess.luas.presentation.model.Compose
import com.wedgess.luas.presentation.model.DropdownItem

@Composable
fun DartForecastContent(
    setRefreshAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    dartForecastViewModel: DartForecastViewModel = hiltViewModel()
) {
    val uiResult by dartForecastViewModel.uiResult.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        setRefreshAction { dartForecastViewModel.onEvent(DartForecastTabContract.Event.OnRefresh) }
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
            Column(modifier) {
                RefreshProgressIndicator(
                    cycleKey = uiState.fetchedAt,
                    intervalMillis = DartForecastTabContract.REFRESH_INTERVAL_MILLIS
                )
                DartListContent(
                    uiState = uiState,
                    onStopSelect = { stop ->
                        dartForecastViewModel.onEvent(
                            DartForecastTabContract.Event.OnStationSelected(stop.text)
                        )
                    },
                    onRowClick = { row ->
                        dartForecastViewModel.onEvent(
                            DartForecastTabContract.Event.OnShowAlarmDialog(
                                tripCode = row.trainCode,
                                operatingDate = row.operatingDate,
                                dueInMins = row.dueIn,
                                destination = row.destination,
                                direction = row.direction
                            )
                        )
                    },
                    onCancelAlarm = { dartForecastViewModel.onEvent(DartForecastTabContract.Event.OnCancelAlarm(it)) },
                    onEvent = dartForecastViewModel::onEvent,
                    onSectionToggle = { sectionId ->
                        dartForecastViewModel.onEvent(
                            DartForecastTabContract.Event.OnToggleSection(sectionId)
                        )
                    }
                )
            }
        }
    )
}

@SuppressLint("ComposeModifierMissing")
@Suppress("LongParameterList") // Stateless list: one callback per user action.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DartListContent(
    uiState: DartForecastTabContract.UiState,
    onSectionToggle: (Long) -> Unit,
    onStopSelect: (DropdownItem) -> Unit,
    onRowClick: (DartForecastSectionRowData) -> Unit,
    onCancelAlarm: (String) -> Unit,
    onEvent: (DartForecastTabContract.Event) -> Unit
) {
    uiState.alarmDraft?.let { draft ->
        NotificationTimeDialog(
            currentMinutes = draft.notifyMinutesBefore,
            dueInMinutes = draft.dueInMins,
            onMinutesChange = { onEvent(DartForecastTabContract.Event.OnAlarmMinutesChanged(it)) },
            onConfirm = { onEvent(DartForecastTabContract.Event.OnStartAlarm(it)) },
            onDismiss = { onEvent(DartForecastTabContract.Event.OnDismissAlarmDialog) },
            exactAlarmsAllowed = uiState.alarmCapabilities.exactAlarms,
            notificationsAllowed = uiState.alarmCapabilities.notifications,
            onOpenSettings = { onEvent(DartForecastTabContract.Event.OnOpenExactAlarmSettings) }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.isStale) {
            StaleDataBanner(lastUpdated = uiState.fetchedAt)
        }
        StationPicker(
            label = "Station",
            selected = uiState.selectedStation,
            options = uiState.dropdownOptions,
            favourites = uiState.stationPreferences.favourites,
            recents = uiState.stationPreferences.recents,
            onSelect = onStopSelect,
            onToggleFavourite = { name, favourite ->
                onEvent(DartForecastTabContract.Event.OnToggleFavourite(name, favourite))
            }
        )
        uiState.activeAlarms.forEach { alarm ->
            LuasForecastAlarmRow(alarm = alarm, onCancelAlarm = onCancelAlarm)
        }
        uiState.sectionedListState.sections.flatMap { it.items }.map { it.content }
            .minByOrNull { it.dueIn }?.let { next ->
                NextDepartureHeader(
                    destination = next.destination,
                    dueMins = next.dueIn,
                    expectedTime = next.expectedAt,
                    stale = uiState.isStale
                )
            }
        SectionedList(
            modifier = Modifier.fillMaxSize(),
            sectionHeader = { DartForecastHeader() },
            state = uiState.sectionedListState,
            onSectionToggle = onSectionToggle
        ) { rowData ->
            DartForecastItemRow(
                destination = rowData.destination,
                eta = rowData.expectedAt,
                dueIn = rowData.dueIn,
                scheduledTime = rowData.scheduledAt,
                late = rowData.late,
                showCountdown = !uiState.isStale,
                onClick = if (uiState.isStale) null else ({ onRowClick(rowData) })
            )
        }
    }
}
