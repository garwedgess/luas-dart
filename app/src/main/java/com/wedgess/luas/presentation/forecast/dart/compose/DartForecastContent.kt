package com.wedgess.luas.presentation.forecast.dart.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wedgess.luas.presentation.components.EmptyContent
import com.wedgess.luas.presentation.components.ErrorContent
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.components.OfflineContent
import com.wedgess.luas.presentation.components.StaleDataBanner
import com.wedgess.luas.presentation.components.sectionedlist.SectionedList
import com.wedgess.luas.presentation.components.stationpicker.StationPicker
import com.wedgess.luas.presentation.forecast.compose.ForecastRefreshCycle
import com.wedgess.luas.presentation.forecast.compose.ForecastReminders
import com.wedgess.luas.presentation.forecast.compose.ForecastStationStatus
import com.wedgess.luas.presentation.forecast.compose.hasActiveReminder
import com.wedgess.luas.presentation.forecast.dart.DartForecastTabContract
import com.wedgess.luas.presentation.forecast.dart.model.DartForecastSectionRowData
import com.wedgess.luas.presentation.forecast.dart.viewmodel.DartForecastViewModel
import com.wedgess.luas.presentation.forecast.luastab.compose.dialogs.NotificationTimeDialog
import com.wedgess.luas.presentation.model.Compose
import com.wedgess.luas.presentation.model.DropdownItem
import com.wedgess.luas.presentation.model.currentState
import com.wedgess.luas.presentation.navigation.reminder.ReminderTarget
import com.wedgess.luas.presentation.navigation.reminder.rememberReminderHighlight
import com.wedgess.luas.presentation.navigation.reminder.reminderHighlight

@Composable
fun DartForecastContent(
    setRefreshAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    dartForecastViewModel: DartForecastViewModel = hiltViewModel(),
    reminderTarget: ReminderTarget? = null,
    onReminderComplete: (String, Boolean) -> Unit = { _, _ -> },
    onRefreshCycleChange: (ForecastRefreshCycle?) -> Unit = {}
) {
    val uiResult by dartForecastViewModel.uiResult.collectAsStateWithLifecycle()
    val cycle = uiResult.currentState()?.takeUnless { it.isStale }?.let {
        ForecastRefreshCycle(it.fetchedAt, DartForecastTabContract.REFRESH_INTERVAL_MILLIS)
    }
    LaunchedEffect(cycle, onRefreshCycleChange) { onRefreshCycleChange(cycle) }
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
        onOffline = {
            OfflineContent(onRetry = { dartForecastViewModel.onEvent(DartForecastTabContract.Event.OnRefresh) })
        },
        onEmpty = {
            EmptyContent(it)
        },
        onSuccess = { uiState ->
            Column(modifier) {
                DartListContent(
                    uiState = uiState,
                    showReminders = false,
                    reminderTarget = reminderTarget,
                    onReminderComplete = onReminderComplete,
                    onStopSelect = { stop ->
                        reminderTarget?.let { onReminderComplete(it.token, false) }
                        dartForecastViewModel.onEvent(
                            DartForecastTabContract.Event.OnStationSelected(stop.text)
                        )
                    },
                    onRowClick = { row ->
                        dartForecastViewModel.onEvent(
                            DartForecastTabContract.Event.OnShowAlarmDialog(
                                stationCode = uiState.forecastStationCode.orEmpty(),
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
    onEvent: (DartForecastTabContract.Event) -> Unit,
    reminderTarget: ReminderTarget? = null,
    onReminderComplete: (String, Boolean) -> Unit = { _, _ -> },
    showReminders: Boolean = true
) {
    val listState = rememberLazyListState()
    val target = reminderTarget?.takeIf { it.isForDartStation(uiState.forecastStationCode) }
    val sections = uiState.sectionedListState.sections
    val sectionIndex = sections.indexOfFirst { section -> section.items.any { target?.matches(it.content) == true } }
    val section = sections.getOrNull(sectionIndex)
    val expanded = section?.let { uiState.sectionedListState.expandedSections[it.id] != false } ?: true
    val toggleSection by rememberUpdatedState(onSectionToggle)
    LaunchedEffect(target?.token, section?.id, expanded) {
        if (section != null && !expanded) toggleSection(section.id)
    }
    val rowIndex = section?.items?.indexOfFirst { target?.matches(it.content) == true } ?: -1
    val listIndex = if (rowIndex >= 0 && expanded) {
        sections.take(sectionIndex).sumOf { it.items.size + 1 } + 1 + rowIndex
    } else {
        null
    }
    val highlight = rememberReminderHighlight(
        target,
        uiState.isStale || !expanded,
        listIndex,
        listState,
        onReminderComplete
    )
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            StationPicker(
                modifier = Modifier.weight(1f),
                label = "Station",
                transport = com.wedgess.luas.domain.model.TransportType.DART,
                selected = uiState.selectedStation,
                options = uiState.dropdownOptions,
                favourites = uiState.stationPreferences.favourites,
                recents = uiState.stationPreferences.recents,
                onSelect = onStopSelect,
                onToggleFavourite = { name, favourite ->
                    onEvent(DartForecastTabContract.Event.OnToggleFavourite(name, favourite))
                },
                supportingContent = {
                    ForecastStationStatus(
                        uiState.fetchedAt,
                        uiState.isStale,
                        DartForecastTabContract.REFRESH_INTERVAL_MILLIS
                    )
                }
            )
            if (showReminders) ForecastReminders(uiState.activeAlarms, onCancelAlarm)
        }
        if (uiState.isStale) StaleDataBanner(uiState.fetchedAt)
        SectionedList(
            modifier = Modifier.weight(1f),
            state = uiState.sectionedListState,
            listState = listState,
            onSectionToggle = onSectionToggle
        ) { rowData ->
            DartForecastItemRow(
                modifier = Modifier.reminderHighlight(highlight && target?.matches(rowData) == true),
                destination = rowData.destination,
                eta = rowData.expectedAt,
                dueIn = rowData.dueIn,
                scheduledTime = rowData.scheduledAt,
                late = rowData.late,
                showCountdown = !uiState.isStale,
                reminderSet = rowData.hasActiveReminder(uiState.activeAlarms, uiState.forecastStationCode),
                onClick = if (uiState.isStale) null else ({ onRowClick(rowData) })
            )
        }
    }
}
