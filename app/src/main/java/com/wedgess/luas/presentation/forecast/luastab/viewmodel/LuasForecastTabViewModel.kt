package com.wedgess.luas.presentation.forecast.luastab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wedgess.luas.di.ForecastTabViewModelFactory
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.RefreshMode
import com.wedgess.luas.domain.model.RefreshState
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.usecase.CanScheduleExactAlarmsUseCase
import com.wedgess.luas.domain.usecase.CancelAlarmUseCase
import com.wedgess.luas.domain.usecase.FetchLuasStopForecastUseCase
import com.wedgess.luas.domain.usecase.FetchIsAlarmRunningUseCase
import com.wedgess.luas.domain.usecase.FetchSelectedLuasStopUseCase
import com.wedgess.luas.domain.usecase.FetchLuasStopsUseCase
import com.wedgess.luas.domain.usecase.RequestExactAlarmPermissionUseCase
import com.wedgess.luas.domain.usecase.ScheduleAlarmUseCase
import com.wedgess.luas.domain.usecase.UpdateSelectedLuasStopUseCase
import com.wedgess.luas.presentation.forecast.luastab.LuasForecastTabContract
import com.wedgess.luas.presentation.forecast.luastab.extensions.toEntity
import com.wedgess.luas.presentation.forecast.luastab.extensions.triggerTimeSeconds
import com.wedgess.luas.presentation.forecast.luastab.model.ForecastTabDialogState
import com.wedgess.luas.presentation.forecast.luastab.model.NotificationState
import com.wedgess.luas.presentation.model.UiResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltViewModel(assistedFactory = ForecastTabViewModelFactory::class)
class LuasForecastTabViewModel @AssistedInject constructor(
    @Assisted val luasLine: LuasLineEntity,
    fetchLuasStopsUseCase: FetchLuasStopsUseCase,
    private val fetchLuasStopForecastUseCase: FetchLuasStopForecastUseCase,
    private val updateSelectedLuasStopUseCase: UpdateSelectedLuasStopUseCase,
    private val canScheduleExactAlarmsUseCase: CanScheduleExactAlarmsUseCase,
    private val cancelAlarmUseCase: CancelAlarmUseCase,
    private val scheduleAlarmUseCase: ScheduleAlarmUseCase,
    private val requestExactAlarmPermissionUseCase: RequestExactAlarmPermissionUseCase,
    isAlarmRunningUseCase: FetchIsAlarmRunningUseCase,
    fetchSelectedLuasStopUseCase: FetchSelectedLuasStopUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LuasForecastTabContract.UiState())
    private var eventTriggerTime = 0L
    private var notificationTimerJob: Job? = null

    private val stopsAndStationFlow = combine(
        fetchLuasStopsUseCase(luasLine),
        fetchSelectedLuasStopUseCase(luasLine),
        isAlarmRunningUseCase()
    ) { stopsResult, currentSelectedStop, alarmIsRunning ->
        stopsResult.mapCatching { stops ->
            val selectedStop = if (currentSelectedStop.isBlank()) {
                stops.firstOrNull() ?: LuasStopEntity.initial()
            } else {
                stops.firstOrNull { it.abbreviation == currentSelectedStop } ?: LuasStopEntity.initial()
            }
            Triple(stops, selectedStop, alarmIsRunning)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiResult = stopsAndStationFlow
        .flatMapLatest { stopsResult: Result<Triple<List<LuasStopEntity>, LuasStopEntity, Boolean>> ->
            stopsResult.fold(
                onSuccess = { (stops, selectedStop, alarmIsRunning) ->
                    _uiState.update {
                        it.copy(
                            stops = stops,
                            selectedStop = selectedStop,
                            alarmIsRunning = alarmIsRunning
                        )
                    }

                    if (stops.isEmpty()) {
                        flowOf(UiResult.Empty("No stops found"))
                    } else {
                        fetchLuasStopForecastUseCase(selectedStop.abbreviation)
                            .map { forecastRefreshResult ->
                                when (forecastRefreshResult) {
                                    is RefreshState.Error -> UiResult.Error(
                                        forecastRefreshResult.exception.message
                                            ?: "Failed to fetch forecast"
                                    )

                                    is RefreshState.Success -> {
                                        val updatedState = _uiState.updateAndGet { currentState ->
                                            currentState.copy(
                                                refreshProgress = forecastRefreshResult.progress,
                                                forecast = forecastRefreshResult.data
                                            )
                                        }

                                        UiResult.Success(updatedState)
                                    }
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

    fun onEvent(event: LuasForecastTabContract.Event) {
        when (event) {
            is LuasForecastTabContract.Event.OnStopSelected -> onStopSelected((event.stopAbrv))
            LuasForecastTabContract.Event.OnRefresh -> fetchLuasStopForecastUseCase.refresh(RefreshMode.MANUAL)
            LuasForecastTabContract.Event.OnDismissDialog -> _uiState.update {
                it.copy(dialog = ForecastTabDialogState.None)
            }

            LuasForecastTabContract.Event.OnShowTravelUpdatesDialog -> _uiState.update {
                it.copy(dialog = ForecastTabDialogState.TravelUpdatesAlert)
            }

            is LuasForecastTabContract.Event.OnStartNotification -> _uiState.updateAndGet {
                it.copy(dialog = ForecastTabDialogState.None)
            }.also {
                scheduleNotification(it.notificationState.copy(notifyMinutesBefore = event.minutes))
            }

            LuasForecastTabContract.Event.OnStopNotification -> stopNotification()

            is LuasForecastTabContract.Event.OnNotificationMinutesChanged -> _uiState.update {
                it.copy(notificationState = it.notificationState.copy(notifyMinutesBefore = event.minutes))
            }

            is LuasForecastTabContract.Event.OnShowNotificationsDialog -> if (event.dueInMins > 0) {
                showNotificationDialog(
                    event.dueInMins,
                    event.destination
                ).also {
                    eventTriggerTime = System.currentTimeMillis()
                }
            }
        }
    }

    private fun startTimer(timeInMinutes: Int) {
        notificationTimerJob?.cancel()
        notificationTimerJob = viewModelScope.launch {
            var minutesRemaining = timeInMinutes
            while (minutesRemaining >= 0 && isActive) {
                _uiState.update {
                    it.copy(notificationState = it.notificationState.copy(dueInMins = minutesRemaining))
                }
                delay(TimeUnit.MINUTES.toMillis(1))
                minutesRemaining--
            }
            cancel()
        }
    }

    private fun showNotificationDialog(dueInMins: Int, destination: String) {
        if (canScheduleExactAlarmsUseCase.invoke()) {
            _uiState.update {
                val notificationState = NotificationState(
                    dueInMins = dueInMins,
                    destination = destination,
                    station = it.selectedStop.name,
                    notifyMinutesBefore = dueInMins
                )
                Timber.d("Notification State: $notificationState, ${_uiState.value}")
                it.copy(
                    notificationState = notificationState,
                    dialog = ForecastTabDialogState.Notification
                )
            }
        } else {
            requestExactAlarmPermissionUseCase()
        }
    }

    private fun stopNotification() = cancelAlarmUseCase.invoke()

    private fun scheduleNotification(notificationState: NotificationState) {
        viewModelScope.launch {
            scheduleAlarmUseCase(
                secondsFromNow = notificationState.triggerTimeSeconds(eventTriggerTime),
                luasNotificationEntity = notificationState.toEntity()
            ).also {
                eventTriggerTime = 0
                startTimer(notificationState.dueInMins)
            }
        }
    }

    private fun onStopSelected(stop: LuasStopEntity) {
        viewModelScope.launch {
            updateSelectedLuasStopUseCase(stop.abbreviation, stop.line)
        }
    }
}
