package com.wedgess.luas.presentation.forecast.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wedgess.luas.di.ForecastTabViewModelFactory
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.domain.usecase.FetchForecastUseCase
import com.wedgess.luas.domain.usecase.FetchStopsUseCase
import com.wedgess.luas.presentation.forecast.ForecastContract
import com.wedgess.luas.presentation.model.UiResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel(assistedFactory = ForecastTabViewModelFactory::class)
class ForecastViewModel @AssistedInject constructor(
    @Assisted val luasLine: LuasLineEntity,
    fetchStopsUseCase: FetchStopsUseCase,
    fetchForecastUseCase: FetchForecastUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForecastContract.UiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiResult = _uiState
        .combine(fetchStopsUseCase(luasLine)) { state, stopsResult ->
            stopsResult.mapCatching { stops ->
                val selectedStop = if (state.selectedStop.abbreviation.isBlank()) {
                    stops.firstOrNull() ?: StopEntity.initial()
                } else {
                    state.selectedStop
                }
                state.copy(stops = stops, selectedStop = selectedStop)
            }
        }
        .flatMapLatest { stopsResult: Result<ForecastContract.UiState> ->
            stopsResult.fold(
                onSuccess = { stopsState ->
                    if (stopsState.stops.isEmpty()) {
                        flowOf(UiResult.Empty("No stops found"))
                    } else {
                        fetchForecastUseCase(stopsState.selectedStop.abbreviation)
                            .map { forecastResult ->
                                forecastResult.fold(
                                    onSuccess = { forecast ->
                                        UiResult.Success(
                                            stopsState.copy(
                                                forecast = forecast
                                            )
                                        )
                                    },
                                    onFailure = { throwable ->
                                        UiResult.Error(
                                            throwable.message ?: "Failed to fetch forecast"
                                        )
                                    }
                                )
                            }
                    }
                },
                onFailure = { throwable ->
                    flowOf(UiResult.Error(throwable.message ?: "Failed to fetch stops"))
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResult.Loading)


    fun onEvent(event: ForecastContract.Event) {
        when (event) {
            is ForecastContract.Event.OnStopSelected -> onStopSelected((event.stopAbrv))
        }
    }

    private fun onStopSelected(stop: StopEntity) {
        _uiState.update { it.copy(selectedStop = stop) }
    }

}