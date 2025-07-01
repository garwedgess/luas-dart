package com.wedgess.luas.presentation.forecast.dart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wedgess.luas.domain.model.DartDirectionEntity
import com.wedgess.luas.domain.model.DartLocationTypeEntity
import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.domain.model.DartStationForecastEntity
import com.wedgess.luas.domain.model.RefreshMode
import com.wedgess.luas.domain.model.RefreshState
import com.wedgess.luas.domain.usecase.FetchAllDartStationsUseCase
import com.wedgess.luas.domain.usecase.FetchDartStationForecastUseCase
import com.wedgess.luas.domain.usecase.FetchSelectedDartStationUseCase
import com.wedgess.luas.domain.usecase.UpdateSelectedDartStationUseCase
import com.wedgess.luas.presentation.components.sectionedlist.model.Section
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionItem
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionedListState
import com.wedgess.luas.presentation.forecast.dart.DartForecastTabContract
import com.wedgess.luas.presentation.forecast.dart.extensions.toDropdownItem
import com.wedgess.luas.presentation.forecast.dart.model.DartForecastSectionRowData
import com.wedgess.luas.presentation.model.UiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DartForecastViewModel @Inject constructor(
    fetchAllDartStationsUseCase: FetchAllDartStationsUseCase,
    private val fetchDartStationForecastUseCase: FetchDartStationForecastUseCase,
    private val updateSelectedDartStationsUseCase: UpdateSelectedDartStationUseCase,
    fetchSelectedDartStationsUseCase: FetchSelectedDartStationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DartForecastTabContract.UiState())
    private val _expandedSections = MutableStateFlow(persistentMapOf<Long, Boolean>())

    private val stopsAndStationFlow = fetchAllDartStationsUseCase().combine(
        fetchSelectedDartStationsUseCase()
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
                        fetchDartStationForecastUseCase(selectedStop.code)
                            .combine(_expandedSections) { forecastRefreshResult, expandedSections ->
                                when (forecastRefreshResult) {
                                    is RefreshState.Error -> UiResult.Error(
                                        forecastRefreshResult.exception.message
                                            ?: "Failed to fetch forecast"
                                    )

                                    is RefreshState.Success -> {
                                        val updatedState = _uiState.updateAndGet { currentState ->
                                            currentState.copy(
                                                refreshProgress = forecastRefreshResult.progress,
                                                sectionedListState = forecastRefreshResult.data.toSectionListState(
                                                    expandedSections
                                                )
                                            )
                                        }

                                        UiResult.Success(updatedState)
                                    }
                                }
                            }
                    }
                },
                onFailure = { throwable ->
                    flowOf(UiResult.Error(throwable.message ?: "Failed to fetch stations"))
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResult.Loading)

    private fun List<DartStationForecastEntity>.toSectionListState(
        expandedSections: PersistentMap<Long, Boolean>
    ): SectionedListState<DartForecastSectionRowData> {
        // Group existing forecasts by direction
        val forecastsByDirection = this.groupBy { it.direction }

        // Create sections for ALL directions, not just the ones with data
        val sections = DartDirectionEntity.entries
            .filter { it != DartDirectionEntity.UNKNOWN } // Exclude UNKNOWN if you don't want it shown
            .map { direction ->
                val forecasts = forecastsByDirection[direction] ?: emptyList()

                Section(
                    id = direction.id,
                    title = direction.key,
                    subTitle = if (forecasts.isEmpty()) {
                        "No trains"
                    } else {
                        "${forecasts.size} ${if (forecasts.size == 1) "train" else "trains"}"
                    },
                    items = forecasts.map { forecast ->
                        SectionItem(
                            id = forecast.trainCode,
                            content = DartForecastSectionRowData(
                                trainCode = forecast.trainCode,
                                dueIn = forecast.dueIn,
                                scheduledAt = when (forecast.locationType) {
                                    DartLocationTypeEntity.ORIGIN -> forecast.schDepart
                                    DartLocationTypeEntity.DESTINATION,
                                    DartLocationTypeEntity.STOP -> forecast.schArrival
                                },
                                destination = forecast.destination,
                                status = forecast.status,
                                lastLocation = forecast.lastLocation,
                                late = forecast.late,
                                expectedAt = when (forecast.locationType) {
                                    DartLocationTypeEntity.ORIGIN -> forecast.expDepart
                                    DartLocationTypeEntity.DESTINATION,
                                    DartLocationTypeEntity.STOP -> forecast.expArrival
                                },
                                direction = forecast.direction
                            )
                        )
                    }.toImmutableList()
                )
            }
            .toImmutableList()

        return SectionedListState(
            sections = sections,
            expandedSections = expandedSections
        )
    }

    fun onEvent(event: DartForecastTabContract.Event) {
        when (event) {
            is DartForecastTabContract.Event.OnStationSelected -> onStopSelected(event.stationName)
            DartForecastTabContract.Event.OnRefresh -> fetchDartStationForecastUseCase.refresh(RefreshMode.MANUAL)
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
            updateSelectedDartStationsUseCase(stationName)
        }
    }
}
