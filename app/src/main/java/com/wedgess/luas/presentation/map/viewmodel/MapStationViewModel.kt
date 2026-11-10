package com.wedgess.luas.presentation.map.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wedgess.luas.domain.isOffline
import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.model.displayable
import com.wedgess.luas.domain.pollFlow
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.usecase.FetchSelectedTransportTypeUseCase
import com.wedgess.luas.presentation.map.model.MapStationState
import com.wedgess.luas.presentation.model.UiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import javax.inject.Inject

private const val LUAS_REFRESH_MILLIS = 15_000L
private const val DART_REFRESH_MILLIS = 40_000L

/** Only polls while the map is visible and a station is selected. Selection changes cancel the old request. */
@HiltViewModel
class MapStationViewModel @Inject constructor(
    private val luasRepository: LuasRepository,
    private val dartRepository: DartRepository,
    fetchSelectedTransportTypeUseCase: FetchSelectedTransportTypeUseCase,
    private val clock: Clock
) : ViewModel() {
    private val selected = MutableStateFlow<LocationEntity?>(null)
    private val refresh =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    @OptIn(ExperimentalCoroutinesApi::class)
    val stationState = combine(selected, fetchSelectedTransportTypeUseCase()) { station, transport ->
        station?.takeIf { (it is LocationEntity.Dart) == (transport == TransportType.DART) }
    }.flatMapLatest { station ->
        if (station == null) flowOf(null) else departures(station)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), null)

    fun select(station: LocationEntity) {
        selected.value = station.takeIf { it.stationCode.isNotBlank() }
    }

    fun dismiss() {
        selected.value = null
    }

    fun retry() {
        refresh.tryEmit(Unit)
    }

    private fun departures(station: LocationEntity) = flow {
        emit(MapStationState(station))
        val dart = station is LocationEntity.Dart
        val cached = if (dart) {
            dartRepository.cachedForecast(station.stationCode)
        } else {
            luasRepository.cachedForecast(station.stationCode)
        }
        emitAll(
            pollFlow(
                intervalMillis = if (dart) DART_REFRESH_MILLIS else LUAS_REFRESH_MILLIS,
                refresh = refresh,
                initial = cached
            ) {
                if (dart) {
                    dartRepository.fetchForecast(
                        station.stationCode
                    )
                } else {
                    luasRepository.fetchForecast(station.stationCode)
                }
            }.map { polled ->
                val displayable = polled.displayable(clock.instant())
                val result = when {
                    displayable != null -> UiResult.Success(displayable)
                    polled.error?.isOffline() == true -> UiResult.Offline("")
                    else -> UiResult.Error("")
                }
                MapStationState(station, result, isStale = !polled.isLive)
            }
        )
    }
}
