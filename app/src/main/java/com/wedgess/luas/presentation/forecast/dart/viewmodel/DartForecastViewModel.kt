package com.wedgess.luas.presentation.forecast.dart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.displayable
import com.wedgess.luas.domain.pollFlow
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.repository.PreferencesRepository
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
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class DartForecastViewModel @Inject constructor(
    private val dartRepository: DartRepository,
    private val preferencesRepository: PreferencesRepository,
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
                stations.firstOrNull { it.name == currentSelectedStop } ?: DartStationEntity.initial()
            }
            Pair(stations, selectedStation)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiResult = stopsAndStationFlow
        .flatMapLatest { stopsResult: Result<Pair<List<DartStationEntity>, DartStationEntity>> ->
            stopsResult.fold(
                onSuccess = { (stops, selectedStop) ->
                    _uiState.update {
                        it.copy(
                            stations = stops.toImmutableList(),
                            selectedStation = selectedStop.toDropdownItem(),
                            dropdownOptions = stops.map { it.toDropdownItem() }.toImmutableList()
                        )
                    }

                    if (stops.isEmpty()) {
                        flowOf(UiResult.Empty("No stops found"))
                    } else {
                        pollFlow(DartForecastTabContract.REFRESH_INTERVAL_MILLIS, refreshTrigger) {
                            dartRepository.fetchForecast(selectedStop.code)
                        }.combine(_expandedSections) { polled, expandedSections ->
                            val departures = polled.displayable(clock.instant())
                            if (departures == null) {
                                UiResult.Error(polled.error?.message ?: "Failed to fetch forecast")
                            } else {
                                UiResult.Success(
                                    _uiState.updateAndGet {
                                        it.copy(
                                            isStale = polled.error != null,
                                            fetchedAt = departures.fetchedAt,
                                            sectionedListState = departures.toSectionListState(expandedSections)
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                onFailure = { throwable ->
                    flowOf(UiResult.Error(throwable.message ?: "Failed to fetch stations"))
                }
            )
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
                            direction = departure.direction
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
            is DartForecastTabContract.Event.OnToggleSection -> {
                _expandedSections.update { currentMap ->
                    val currentState = currentMap[event.id] ?: true
                    currentMap.put(event.id, !currentState)
                }
            }
        }
    }

    private fun onStopSelected(stationName: String) {
        viewModelScope.launch {
            preferencesRepository.updateSelectedDartStation(stationName)
        }
    }
}

private val DART_DIRECTIONS = listOf(DepartureDirection.NORTHBOUND, DepartureDirection.SOUTHBOUND)
