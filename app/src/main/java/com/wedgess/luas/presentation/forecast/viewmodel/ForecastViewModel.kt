package com.wedgess.luas.presentation.forecast.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wedgess.luas.di.ForecastTabViewModelFactory
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.RefreshMode
import com.wedgess.luas.domain.model.RefreshState
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.domain.usecase.FetchForecastUseCase
import com.wedgess.luas.domain.usecase.FetchStopsUseCase
import com.wedgess.luas.presentation.forecast.ForecastContract
import com.wedgess.luas.presentation.forecast.model.ForecastDialogState
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
    private val fetchForecastUseCase: FetchForecastUseCase
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
                            .map { forecastRefreshResult ->
                                when (forecastRefreshResult) {
                                    is RefreshState.Error -> UiResult.Error(
                                        forecastRefreshResult.exception.message
                                            ?: "Failed to fetch forecast"
                                    )

                                    is RefreshState.Success -> UiResult.Success(
                                        stopsState.copy(
                                            refreshProgress = forecastRefreshResult.progress,
                                            forecast = forecastRefreshResult.data
                                        )
                                    )
                                }
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
            ForecastContract.Event.OnRefresh -> fetchForecastUseCase.refresh(RefreshMode.MANUAL)
            ForecastContract.Event.OnDismissTravelUpdatesDialog -> _uiState.update {
                it.copy(dialog = ForecastDialogState.None)
            }

            ForecastContract.Event.OnShowTravelUpdatesDialog -> _uiState.update {
                it.copy(dialog = ForecastDialogState.TravelUpdatesAlert)
            }
        }
    }

    private fun onStopSelected(stop: StopEntity) {
        _uiState.update { it.copy(selectedStop = stop) }
    }
}
