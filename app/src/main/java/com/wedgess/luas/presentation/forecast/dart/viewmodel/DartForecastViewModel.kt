package com.wedgess.luas.presentation.forecast.dart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wedgess.luas.domain.alarm.AlarmCoordinator
import com.wedgess.luas.domain.isOffline
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmRequest
import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.model.displayable
import com.wedgess.luas.domain.pollFlow
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.repository.StationPreferencesRepository
import com.wedgess.luas.presentation.components.sectionedlist.model.Section
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionItem
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionedListState
import com.wedgess.luas.presentation.extensions.toClockTime
import com.wedgess.luas.presentation.forecast.dart.DartForecastTabContract
import com.wedgess.luas.presentation.forecast.dart.extensions.toDropdownItem
import com.wedgess.luas.presentation.forecast.dart.model.DartForecastSectionRowData
import com.wedgess.luas.presentation.model.UiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DartForecastViewModel @Inject constructor(
    private val dartRepository: DartRepository,
    private val preferencesRepository: PreferencesRepository,
    private val alarmCoordinator: AlarmCoordinator,
    private val stationPreferencesRepository: StationPreferencesRepository,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(DartForecastTabContract.UiState())
    private val _expandedSections = MutableStateFlow(persistentMapOf<Long, Boolean>())
    private val refreshTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val stopsAndStationFlow = dartRepository.fetchStations().combine(
        preferencesRepository.fetchSelectedDartStation()
    ) { stopsResult, currentSelectedStop ->
        stopsResult.mapCatching { stations ->
            val selectedStation = if (currentSelectedStop.isBlank()) {
                stations.firstOrNull() ?: DartStationEntity.initial()
            } else {
                stations.firstOrNull { it.name == currentSelectedStop }
                    ?: stations.firstOrNull() ?: DartStationEntity.initial()
            }
            Pair(stations, selectedStation)
        }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val forecastResult = stopsAndStationFlow
        .flatMapLatest { stopsResult: Result<Pair<List<DartStationEntity>, DartStationEntity>> ->
            stopsResult.fold(
                onSuccess = { (stops, selectedStop) ->
                    _uiState.update {
                        it.copy(
                            stations = stops.toImmutableList(),
                            selectedStation = selectedStop.toDropdownItem(),
                            forecastStationCode = null,
                            sectionedListState = SectionedListState(sections = persistentListOf()),
                            fetchedAt = java.time.Instant.EPOCH,
                            isStale = true,
                            alarmDraft = null,
                            dropdownOptions = stops.map { it.toDropdownItem() }.toImmutableList()
                        )
                    }

                    if (stops.isEmpty()) {
                        flowOf(UiResult.Empty("No stops found"))
                    } else {
                        flow {
                            emitAll(
                                pollFlow(
                                    intervalMillis = DartForecastTabContract.REFRESH_INTERVAL_MILLIS,
                                    refresh = refreshTrigger,
                                    initial = dartRepository.cachedForecast(selectedStop.code)
                                ) { dartRepository.fetchForecast(selectedStop.code) }
                            )
                        }.combine(_expandedSections) { polled, expandedSections ->
                            val departures = polled.displayable(clock.instant())
                            if (departures == null) {
                                polled.error.toErrorResult()
                            } else {
                                UiResult.Success(
                                    _uiState.updateAndGet {
                                        it.copy(
                                            isStale = !polled.isLive,
                                            fetchedAt = departures.fetchedAt,
                                            forecastStationCode = selectedStop.code,
                                            sectionedListState = departures.toSectionListState(expandedSections)
                                        )
                                    }
                                )
                            }
                        }.onStart { emit(UiResult.Loading) }
                    }
                },
                onFailure = { throwable ->
                    flowOf(UiResult.Error(throwable.message ?: "Failed to fetch stations"))
                }
            )
        }

    private val activeDartAlarms = alarmCoordinator.observeActive()
        .map { alarms -> alarms.filter { it.transport == TransportType.DART }.toImmutableList() }

    // Dialog and alarm changes re-emit immediately; they must not wait for the next poll.
    val uiResult = combine(
        forecastResult,
        _uiState,
        activeDartAlarms,
        stationPreferencesRepository.observe(TransportType.DART)
    ) { result, state, alarms, preferences ->
        if (result is UiResult.Success) {
            val selectedCode = state.stations.firstOrNull { it.name == state.selectedStation.text }?.code
            if (state.forecastStationCode != null && state.forecastStationCode == selectedCode) {
                UiResult.Success(state.copy(activeAlarms = alarms, stationPreferences = preferences))
            } else {
                UiResult.Loading
            }
        } else {
            result
        }
    }
        .stateIn(
            viewModelScope,
            // Drop the cached value once unsubscribed so a resumed screen never shows old data as live.
            SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000, replayExpirationMillis = 0),
            UiResult.Loading
        )

    private fun Departures.toSectionListState(
        expandedSections: PersistentMap<Long, Boolean>
    ): SectionedListState<DartForecastSectionRowData> {
        val sections = DART_DIRECTIONS.map { direction ->
            val departures = byDirection(direction)

            Section(
                id = direction.id,
                title = direction.key,
                subTitle = if (departures.isEmpty()) {
                    "No trains"
                } else {
                    "${departures.size} ${if (departures.size == 1) "train" else "trains"}"
                },
                items = departures.map { departure ->
                    SectionItem(
                        id = departure.tripCode.orEmpty(),
                        content = DartForecastSectionRowData(
                            trainCode = departure.tripCode.orEmpty(),
                            dueIn = departure.dueMins ?: 0,
                            scheduledAt = departure.scheduledAt?.toClockTime().orEmpty(),
                            destination = departure.destination,
                            status = departure.status,
                            lastLocation = departure.lastLocation,
                            late = departure.lateMins,
                            expectedAt = departure.expectedAt?.toClockTime().orEmpty(),
                            direction = departure.direction,
                            operatingDate = departure.operatingDate
                        )
                    )
                }.toImmutableList()
            )
        }.toImmutableList()

        return SectionedListState(
            sections = sections,
            expandedSections = expandedSections
        )
    }

    fun onEvent(event: DartForecastTabContract.Event) {
        when (event) {
            is DartForecastTabContract.Event.OnStationSelected -> onStopSelected(event.stationName)
            DartForecastTabContract.Event.OnRefresh -> refreshTrigger.tryEmit(Unit)
            is DartForecastTabContract.Event.OnToggleFavourite -> viewModelScope.launch {
                stationPreferencesRepository.setFavourite(TransportType.DART, event.stationName, event.favourite)
            }
            is DartForecastTabContract.Event.OnShowAlarmDialog -> showAlarmDialog(event)
            is DartForecastTabContract.Event.OnAlarmMinutesChanged -> _uiState.update { state ->
                state.copy(alarmDraft = state.alarmDraft?.copy(notifyMinutesBefore = event.minutes))
            }
            is DartForecastTabContract.Event.OnStartAlarm -> startAlarm(event.minutes)
            is DartForecastTabContract.Event.OnCancelAlarm -> viewModelScope.launch {
                alarmCoordinator.cancel(event.alarmId)
            }
            DartForecastTabContract.Event.OnDismissAlarmDialog -> _uiState.update { it.copy(alarmDraft = null) }
            DartForecastTabContract.Event.OnOpenExactAlarmSettings -> alarmCoordinator.openExactAlarmSettings()
            is DartForecastTabContract.Event.OnToggleSection -> {
                _expandedSections.update { currentMap ->
                    val currentState = currentMap[event.id] ?: true
                    currentMap.put(event.id, !currentState)
                }
            }
        }
    }

    private fun showAlarmDialog(event: DartForecastTabContract.Event.OnShowAlarmDialog) {
        val state = _uiState.value
        // An alarm must never be derived from a countdown that is no longer live.
        if (event.dueInMins <= 0 || state.isStale || event.tripCode.isBlank()) return
        if (state.forecastStationCode != event.stationCode) return
        val station = state.stations.firstOrNull { it.name == state.selectedStation.text } ?: return
        _uiState.update {
            it.copy(
                alarmDraft = DartForecastTabContract.AlarmDraft(
                    requestId = UUID.randomUUID().toString(),
                    tripCode = event.tripCode,
                    operatingDate = event.operatingDate,
                    destination = event.destination,
                    direction = event.direction,
                    stationName = station.name,
                    stationCode = station.code,
                    dueInMins = event.dueInMins,
                    // The live result's time plus its minutes, not a countdown from whenever the dialog opened.
                    expectedAt = it.fetchedAt.plusSeconds(event.dueInMins * SECONDS_PER_MINUTE),
                    observedAt = it.fetchedAt,
                    notifyMinutesBefore = event.dueInMins
                ),
                alarmCapabilities = alarmCoordinator.capabilities
            )
        }
    }

    private fun startAlarm(minutes: Int) {
        val draft = _uiState.value.alarmDraft ?: return
        _uiState.update { it.copy(alarmDraft = null) }
        viewModelScope.launch {
            alarmCoordinator.create(
                AlarmRequest(
                    requestId = draft.requestId,
                    kind = AlarmKind.SPECIFIC_DEPARTURE,
                    transport = TransportType.DART,
                    stationCode = draft.stationCode,
                    stationName = draft.stationName,
                    direction = draft.direction,
                    destination = draft.destination,
                    notifyMinutesBefore = minutes,
                    tripCode = draft.tripCode,
                    operatingDate = draft.operatingDate,
                    expectedAt = draft.expectedAt,
                    observedAt = draft.observedAt
                )
            )
        }
    }

    private fun onStopSelected(stationName: String) {
        _uiState.update {
            if (stationName == it.selectedStation.text) it else it.copy(isStale = true, alarmDraft = null)
        }
        viewModelScope.launch {
            stationPreferencesRepository.markUsed(TransportType.DART, stationName)
            preferencesRepository.updateSelectedDartStation(stationName)
        }
    }
}

private val DART_DIRECTIONS = listOf(DepartureDirection.NORTHBOUND, DepartureDirection.SOUTHBOUND)

private const val SECONDS_PER_MINUTE = 60L

private fun Throwable?.toErrorResult(): UiResult<Nothing> {
    val message = this?.message ?: "Failed to fetch forecast"
    return if (this?.isOffline() == true) UiResult.Offline(message) else UiResult.Error(message)
}
