package com.wedgess.luas.presentation.forecast.luastab.compose.components

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wedgess.luas.R
import com.wedgess.luas.di.ForecastTabViewModelFactory
import com.wedgess.luas.domain.alarm.AlarmMatch
import com.wedgess.luas.domain.alarm.matchAlarm
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.presentation.components.EmptyContent
import com.wedgess.luas.presentation.components.ErrorContent
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.components.OfflineContent
import com.wedgess.luas.presentation.components.StaleDataBanner
import com.wedgess.luas.presentation.components.sectionedlist.SectionedList
import com.wedgess.luas.presentation.components.sectionedlist.model.Section
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionItem
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionedListState
import com.wedgess.luas.presentation.components.stationpicker.StationPicker
import com.wedgess.luas.presentation.extensions.toClockTime
import com.wedgess.luas.presentation.forecast.compose.ForecastRefreshCycle
import com.wedgess.luas.presentation.forecast.compose.ForecastReminders
import com.wedgess.luas.presentation.forecast.compose.ForecastStationStatus
import com.wedgess.luas.presentation.forecast.compose.luasDeparturesWithReminders
import com.wedgess.luas.presentation.forecast.luastab.LuasForecastTabContract
import com.wedgess.luas.presentation.forecast.luastab.viewmodel.LuasForecastTabViewModel
import com.wedgess.luas.presentation.model.Compose
import com.wedgess.luas.presentation.model.DropdownItem
import com.wedgess.luas.presentation.model.currentState
import com.wedgess.luas.presentation.navigation.reminder.ReminderTarget
import com.wedgess.luas.presentation.navigation.reminder.rememberReminderHighlight
import com.wedgess.luas.presentation.navigation.reminder.reminderHighlight
import com.wedgess.luas.ui.theme.LuasTheme
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import java.time.Instant

@Composable
fun LuasForecastTabContent(
    line: LuasLineEntity,
    onRefreshAction: (() -> Unit) -> Unit,
    onRefreshCycleChange: (ForecastRefreshCycle?) -> Unit = {},
    isActive: Boolean = true,
    reminderTarget: ReminderTarget? = null,
    onReminderComplete: (String, Boolean) -> Unit = { _, _ -> },
    luasForecastTabViewModel: LuasForecastTabViewModel = hiltViewModel(
        key = line.name,
        creationCallback = { factory: ForecastTabViewModelFactory ->
            factory.create(line)
        }
    )
) {
    val uiResult by luasForecastTabViewModel.uiResult.collectAsStateWithLifecycle()

    val cycle = uiResult.currentState()?.takeUnless { it.isStale }?.let {
        ForecastRefreshCycle(it.forecast.fetchedAt, LuasForecastTabContract.REFRESH_INTERVAL_MILLIS)
    }
    LaunchedEffect(cycle, isActive, onRefreshCycleChange) {
        if (isActive) onRefreshCycleChange(cycle)
    }
    LaunchedEffect(isActive, onRefreshAction) {
        if (isActive) onRefreshAction { luasForecastTabViewModel.onEvent(LuasForecastTabContract.Event.OnRefresh) }
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
                mins: Int, destination: String, direction: DepartureDirection, stationCode: String ->
            luasForecastTabViewModel.onEvent(
                LuasForecastTabContract.Event.OnShowNotificationsDialog(stationCode, mins, destination, direction)
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
                showReminders = false,
                reminderTarget = reminderTarget,
                onReminderComplete = onReminderComplete,
                onStopSelect = { stop ->
                    reminderTarget?.let { onReminderComplete(it.token, false) }
                    onStopSelect(stop)
                },
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
    onTramClick: (Int, String, DepartureDirection, String) -> Unit,
    onCancelAlarm: (String) -> Unit,
    reminderTarget: ReminderTarget? = null,
    onReminderComplete: (String, Boolean) -> Unit = { _, _ -> },
    showReminders: Boolean = true
) {
    Column {
        LuasTramList(
            uiState = uiState,
            reminderTarget = reminderTarget,
            onReminderComplete = onReminderComplete,
            onShowTravelUpdatesDialog = onShowTravelUpdatesDialog,
            onStopSelect = onStopSelect,
            onToggleFavourite = onToggleFavourite,
            onTramClick = onTramClick,
            onCancelAlarm = onCancelAlarm,
            showReminders = showReminders
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
    onTramClick: (Int, String, DepartureDirection, String) -> Unit,
    onCancelAlarm: (String) -> Unit,
    reminderTarget: ReminderTarget? = null,
    onReminderComplete: (String, Boolean) -> Unit = { _, _ -> },
    showReminders: Boolean = true
) {
    val departuresWithReminders = luasDeparturesWithReminders(uiState.activeAlarms, uiState.forecast)
    val outbound = uiState.forecast.byDirection(DepartureDirection.OUTBOUND)
    val inbound = uiState.forecast.byDirection(DepartureDirection.INBOUND)
    val target = reminderTarget?.takeIf { it.alarm.stationCode.equals(uiState.forecastStationCode, true) }
    val matched = reminderDeparture(target, uiState)
    val showStatus = hasTravelDisruption(uiState.forecast.message)
    val sections = luasSections(uiState)
    var collapsed by rememberSaveable(uiState.forecastStationCode) { mutableStateOf(emptyList<Long>()) }
    val matchedSection = matched?.let { if (it.direction == DepartureDirection.OUTBOUND) 0L else 1L }
    LaunchedEffect(target?.token, matchedSection) {
        matchedSection?.let { collapsed = collapsed - it }
    }
    val listIndex = when {
        matched == null -> null
        matched.direction == DepartureDirection.OUTBOUND -> 1 + outbound.indexOf(matched)
        else -> outbound.size + 2 + inbound.indexOf(matched)
    }
    val listState = rememberLazyListState()
    val highlight = rememberReminderHighlight(
        target,
        uiState.isStale || matchedSection in collapsed,
        listIndex,
        listState,
        onReminderComplete
    )
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            StationPicker(
                modifier = Modifier.weight(1f),
                label = "Stop",
                selected = uiState.selectedStop,
                options = uiState.dropdownOptions,
                favourites = uiState.stationPreferences.favourites,
                recents = uiState.stationPreferences.recents,
                onSelect = onStopSelect,
                onToggleFavourite = onToggleFavourite,
                supportingContent = {
                    ForecastStationStatus(
                        uiState.forecast.fetchedAt,
                        uiState.isStale,
                        LuasForecastTabContract.REFRESH_INTERVAL_MILLIS
                    )
                }
            )
            if (showReminders) ForecastReminders(uiState.activeAlarms, onCancelAlarm)
        }
        if (uiState.isStale) StaleDataBanner(uiState.forecast.fetchedAt)
        if (showStatus) {
            LuasForecastStatusMessage(uiState.forecast.message, showTravelUpdatesDialog = onShowTravelUpdatesDialog)
        }
        SectionedList(
            modifier = Modifier.weight(1f),
            state = SectionedListState(
                sections,
                sections.associate { it.id to (it.id !in collapsed) }.toPersistentMap()
            ),
            listState = listState,
            onSectionToggle = { id -> collapsed = if (id in collapsed) collapsed - id else collapsed + id }
        ) { tram ->
            LuasForecastItemRow(
                modifier = Modifier.reminderHighlight(highlight && tram == matched),
                dueIn = tram.dueMins ?: -1,
                destination = tram.destination,
                stale = uiState.isStale,
                reminderSet = tram in departuresWithReminders,
                expectedTime = tram.expectedAt?.toClockTime(),
                onRowClick = { mins, destination ->
                    onTramClick(mins, destination, tram.direction, uiState.forecastStationCode.orEmpty())
                }
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

private fun hasTravelDisruption(message: String): Boolean =
    message.isNotBlank() && !message.contains("operating normally", ignoreCase = true)

@Composable
private fun luasSections(uiState: LuasForecastTabContract.UiState) =
    listOf(DepartureDirection.OUTBOUND, DepartureDirection.INBOUND)
        .mapIndexed { index, direction ->
            val trams = uiState.forecast.byDirection(direction)
            Section(
                id = index.toLong(),
                title = stringResource(
                    if (direction == DepartureDirection.OUTBOUND) {
                        R.string.forecast_title_outbound
                    } else {
                        R.string.forecast_title_inbound
                    }
                ),
                subTitle = if (trams.isEmpty()) {
                    stringResource(R.string.forecast_no_trams)
                } else {
                    pluralStringResource(R.plurals.timetable_departure_count, trams.size, trams.size)
                },
                items = trams.mapIndexed { row, tram -> SectionItem("$index-$row", tram) }.toImmutableList()
            )
        }.toImmutableList()

private fun reminderDeparture(target: ReminderTarget?, uiState: LuasForecastTabContract.UiState) =
    target?.let { matchAlarm(it.alarm, uiState.forecast, Instant.now()) as? AlarmMatch.Found }
        ?.takeUnless { it.ambiguous }?.departure
