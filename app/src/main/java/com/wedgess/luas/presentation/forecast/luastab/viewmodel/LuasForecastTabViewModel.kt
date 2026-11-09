package com.wedgess.luas.presentation.forecast.luastab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wedgess.luas.di.ForecastTabViewModelFactory
import com.wedgess.luas.domain.alarm.AlarmCoordinator
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmRequest
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.model.displayable
import com.wedgess.luas.domain.pollFlow
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.presentation.forecast.luastab.LuasForecastTabContract
import com.wedgess.luas.presentation.forecast.luastab.extensions.toDropdownItem
import com.wedgess.luas.presentation.forecast.luastab.model.LuasForecastTabDialogState
import com.wedgess.luas.presentation.forecast.luastab.model.LuasNotificationState
import com.wedgess.luas.presentation.model.UiResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.UUID

private const val SECONDS_PER_MINUTE = 60L

@HiltViewModel(assistedFactory = ForecastTabViewModelFactory::class)
class LuasForecastTabViewModel @AssistedInject constructor(
    @Assisted val luasLine: LuasLineEntity,
    private val luasRepository: LuasRepository,
    private val preferencesRepository: PreferencesRepository,
    private val alarmCoordinator: AlarmCoordinator,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(LuasForecastTabContract.UiState())
    private val refreshTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val stopsAndStationFlow = combine(
        luasRepository.fetchStops(luasLine),
        selectedStopName()
    ) { stopsResult, currentSelectedStopName ->
        stopsResult.mapCatching { stops ->
            val selectedStop = if (currentSelectedStopName.isBlank()) {
                stops.firstOrNull() ?: LuasStopEntity.initial()
            } else {
                stops.firstOrNull { it.name == currentSelectedStopName } ?: LuasStopEntity.initial()
            }
            stops to selectedStop
        }
    }

    /** Loading/empty/error/success of the forecast. The state itself lives in [_uiState]. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val forecastResult: Flow<UiResult<Unit>> = stopsAndStationFlow
        .flatMapLatest { stopsResult: Result<Pair<List<LuasStopEntity>, LuasStopEntity>> ->
            stopsResult.fold(
                onSuccess = { (stops, selectedStop) ->
                    _uiState.update {
                        it.copy(
                            stops = stops.toImmutableList(),
                            dropdownOptions = stops.map { it.toDropdownItem() }.toImmutableList(),
                            selectedStop = selectedStop.toDropdownItem()
                        )
                    }

                    if (stops.isEmpty()) {
                        flowOf(UiResult.Empty("No stops found"))
                    } else {
                        flow {
                            emitAll(
                                pollFlow(
                                    intervalMillis = LuasForecastTabContract.REFRESH_INTERVAL_MILLIS,
                                    refresh = refreshTrigger,
                                    initial = luasRepository.cachedForecast(selectedStop.abbreviation)
                                ) { luasRepository.fetchForecast(selectedStop.abbreviation) }
                            )
                        }.map { polled ->
                            val departures = polled.displayable(clock.instant())
                            if (departures == null) {
                                UiResult.Error(polled.error?.message ?: "Failed to fetch forecast")
                            } else {
                                _uiState.update { it.copy(forecast = departures, isStale = !polled.isLive) }
                                UiResult.Success(Unit)
                            }
                        }
                    }
                },
                onFailure = { throwable ->
                    flowOf(UiResult.Error(throwable.message ?: "Failed to fetch stops"))
                }
            )
        }

    private val activeLuasAlarms = alarmCoordinator.observeActive()
        .map { alarms -> alarms.filter { it.transport == TransportType.LUAS }.toImmutableList() }

    // Dialog and alarm changes re-emit immediately; they must not wait for the next poll.
    val uiResult = combine(forecastResult, _uiState, activeLuasAlarms) { result, state, alarms ->
        when (result) {
            is UiResult.Success -> UiResult.Success(state.copy(activeAlarms = alarms))
            is UiResult.Error -> result
            is UiResult.Empty -> result
            UiResult.Loading -> UiResult.Loading
        }
    }.stateIn(
        viewModelScope,
        // Drop the cached value once unsubscribed so a resumed screen never shows old data as live.
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000, replayExpirationMillis = 0),
        UiResult.Loading
    )

    fun onEvent(event: LuasForecastTabContract.Event) {
        when (event) {
            is LuasForecastTabContract.Event.OnStopSelected -> onStopSelected((event.stopName))

            LuasForecastTabContract.Event.OnRefresh -> refreshTrigger.tryEmit(Unit)
            LuasForecastTabContract.Event.OnDismissDialog -> _uiState.update {
                it.copy(dialog = LuasForecastTabDialogState.None)
            }

            LuasForecastTabContract.Event.OnShowTravelUpdatesDialog -> _uiState.update {
                it.copy(dialog = LuasForecastTabDialogState.TravelUpdatesAlert)
            }

            is LuasForecastTabContract.Event.OnStartNotification -> startAlarm(event.minutes)

            is LuasForecastTabContract.Event.OnStopNotification -> viewModelScope.launch {
                alarmCoordinator.cancel(event.alarmId)
            }

            LuasForecastTabContract.Event.OnOpenExactAlarmSettings -> alarmCoordinator.openExactAlarmSettings()

            is LuasForecastTabContract.Event.OnNotificationMinutesChanged -> _uiState.update {
                it.copy(luasNotificationState = it.luasNotificationState.copy(notifyMinutesBefore = event.minutes))
            }

            // An alarm must never be derived from a countdown that is no longer live.
            is LuasForecastTabContract.Event.OnShowNotificationsDialog -> if (canShowAlarmDialog(event.dueInMins)) {
                showNotificationDialog(event)
            }
        }
    }

    private fun showNotificationDialog(event: LuasForecastTabContract.Event.OnShowNotificationsDialog) {
        _uiState.update {
            val stationName = it.selectedStop.text
            val observedAt = it.forecast.fetchedAt
            it.copy(
                luasNotificationState = LuasNotificationState(
                    requestId = UUID.randomUUID().toString(),
                    dueInMins = event.dueInMins,
                    station = stationName,
                    stationCode = it.stops.firstOrNull { stop -> stop.name == stationName }?.abbreviation.orEmpty(),
                    destination = event.destination,
                    direction = event.direction,
                    // The expected arrival is the live result's time plus its minutes, not a countdown from now.
                    expectedAt = observedAt.plusSeconds(event.dueInMins * SECONDS_PER_MINUTE),
                    observedAt = observedAt,
                    notifyMinutesBefore = event.dueInMins
                ),
                // Permissions are read now so the dialog can say what will be degraded, without prompting.
                alarmCapabilities = alarmCoordinator.capabilities,
                dialog = LuasForecastTabDialogState.Notification
            )
        }
    }

    private fun startAlarm(minutes: Int) {
        val pending = _uiState.value.luasNotificationState
        _uiState.update { it.copy(dialog = LuasForecastTabDialogState.None) }
        val expectedAt = pending.expectedAt ?: return
        viewModelScope.launch {
            alarmCoordinator.create(
                AlarmRequest(
                    requestId = pending.requestId,
                    kind = AlarmKind.SPECIFIC_DEPARTURE,
                    transport = TransportType.LUAS,
                    stationCode = pending.stationCode,
                    stationName = pending.station,
                    direction = pending.direction,
                    destination = pending.destination,
                    notifyMinutesBefore = minutes,
                    expectedAt = expectedAt,
                    observedAt = pending.observedAt ?: expectedAt
                )
            )
        }
    }

    private fun canShowAlarmDialog(dueInMins: Int) = dueInMins > 0 && !_uiState.value.isStale

    private fun onStopSelected(stopName: String) {
        viewModelScope.launch {
            when (luasLine) {
                LuasLineEntity.RED -> preferencesRepository.updateSelectedRedLineStation(stopName)
                LuasLineEntity.GREEN -> preferencesRepository.updateSelectedGreenLineStation(stopName)
            }
        }
    }

    private fun selectedStopName() = when (luasLine) {
        LuasLineEntity.RED -> preferencesRepository.fetchSelectedRedLineStation()
        LuasLineEntity.GREEN -> preferencesRepository.fetchSelectedGreenLineStation()
    }
}
