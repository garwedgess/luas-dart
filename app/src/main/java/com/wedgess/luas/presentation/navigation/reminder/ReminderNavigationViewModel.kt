package com.wedgess.luas.presentation.navigation.reminder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.AlarmConfigRepository
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReminderNavigationViewModel @Inject constructor(
    private val alarms: AlarmConfigRepository,
    private val preferences: PreferencesRepository,
    private val luas: LuasRepository,
    private val dart: DartRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(ReminderNavigationState())
    val state = _state.asStateFlow()
    private var openJob: Job? = null

    init {
        savedState.get<String>(PENDING_ALARM)?.let(::open)
    }

    fun open(alarmId: String) {
        openJob?.cancel()
        savedState[PENDING_ALARM] = alarmId
        _state.value = ReminderNavigationState(loading = true)
        openJob = viewModelScope.launch {
            try {
                val alarm = alarms.get(alarmId).getOrThrow() ?: error("Reminder no longer stored")
                val (selectedAlarm, line) = withTimeout(OPEN_TIMEOUT_MILLIS) { selectStation(alarm) }
                _state.value = ReminderNavigationState(
                    ReminderTarget(UUID.randomUUID().toString(), selectedAlarm, line)
                )
            } catch (_: TimeoutCancellationException) {
                failed()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failed()
            }
        }
    }

    private suspend fun selectStation(alarm: Alarm): Pair<Alarm, LuasLineEntity?> {
        preferences.updateSelectedTransportType(alarm.transport).getOrThrow()
        return when (alarm.transport) {
            TransportType.DART -> {
                val stations = dart.fetchStations().first { it.isFailure || it.getOrNull()?.isNotEmpty() == true }
                    .getOrThrow()
                val station = stations.first { it.code.equals(alarm.stationCode, ignoreCase = true) }
                preferences.updateSelectedDartStation(station.name).getOrThrow()
                alarm.copy(stationName = station.name) to null
            }
            TransportType.LUAS -> {
                val stops = luas.fetchAllStops().first { it.isFailure || it.getOrNull()?.isNotEmpty() == true }
                    .getOrThrow()
                val stop = stops.first { it.abbreviation.equals(alarm.stationCode, ignoreCase = true) }
                when (stop.line) {
                    LuasLineEntity.RED -> preferences.updateSelectedRedLineStation(stop.name).getOrThrow()
                    LuasLineEntity.GREEN -> preferences.updateSelectedGreenLineStation(stop.name).getOrThrow()
                }
                alarm.copy(stationName = stop.name) to stop.line
            }
        }
    }

    fun complete(token: String, unavailable: Boolean) {
        if (_state.value.target?.token != token) return
        savedState.remove<String>(PENDING_ALARM)
        _state.value = ReminderNavigationState(
            error = if (unavailable) ReminderNavigationError.DEPARTURE_UNAVAILABLE else null
        )
    }

    fun cancel() {
        openJob?.cancel()
        savedState.remove<String>(PENDING_ALARM)
        _state.value = ReminderNavigationState()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun failed() {
        savedState.remove<String>(PENDING_ALARM)
        _state.value = ReminderNavigationState(error = ReminderNavigationError.OPEN_FAILED)
    }

    private companion object {
        const val PENDING_ALARM = "pendingReminderAlarm"
        const val OPEN_TIMEOUT_MILLIS = 12_000L
    }
}
