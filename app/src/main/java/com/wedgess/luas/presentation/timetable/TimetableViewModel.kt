package com.wedgess.luas.presentation.timetable

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wedgess.luas.data.utils.extensions.resultOf
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.timetable.TimetableDay
import com.wedgess.luas.domain.timetable.TimetableFeed
import com.wedgess.luas.domain.timetable.TimetableRepository
import com.wedgess.luas.domain.timetable.TimetableStation
import com.wedgess.luas.domain.timetable.TimetableZone
import com.wedgess.luas.domain.usecase.FetchSelectedTransportTypeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class TimetableUiState(
    val transport: TransportType = TransportType.LUAS,
    val date: LocalDate,
    val now: Instant,
    val feed: TimetableFeed? = null,
    val stations: List<TimetableStation> = emptyList(),
    val station: TimetableStation? = null,
    val day: TimetableDay = TimetableDay(false, emptyList()),
    val loading: Boolean = true,
    val updating: Boolean = false,
    val updateFailed: Boolean = false,
    val loadFailed: Boolean = false
)

private data class UpdateState(val updating: Boolean = false, val failed: Boolean = false)

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val repository: TimetableRepository,
    transportUseCase: FetchSelectedTransportTypeUseCase,
    private val clock: Clock,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val date = MutableStateFlow(
        savedState.get<Long>("timetable_date")?.let(LocalDate::ofEpochDay)
            ?: LocalDate.now(clock.withZone(TimetableZone))
    )
    private var lastContent: TimetableUiState? = null
    private val revision = MutableStateFlow(0)
    private val updates = MutableStateFlow<Map<TransportType, UpdateState>>(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val content = combine(transportUseCase(), date, revision) { transport, day, _ -> transport to day }
        .flatMapLatest { (transport, date) ->
            flow {
                val initial = TimetableUiState(transport = transport, date = date, now = clock.instant())
                val previous = lastContent?.takeIf { it.transport == transport }
                if (previous == null) emit(initial)
                val loaded = resultOf {
                    val feed = repository.feed(transport)
                    val stations = repository.stations(transport)
                    val selected = savedState.get<String>(stationKey(transport))
                    val station = stations.firstOrNull { it.id == selected } ?: stations.firstOrNull()
                    val day = station?.let { repository.day(transport, it.id, date) }
                        ?: TimetableDay(false, emptyList())
                    initial.copy(feed = feed, stations = stations, station = station, day = day, loading = false)
                }.getOrElse {
                    previous?.copy(loadFailed = true) ?: initial.copy(loading = false, loadFailed = true)
                }
                lastContent = loaded
                emit(loaded)
            }
        }

    val uiState = combine(content, updates) { state, updates ->
        val status = updates[state.transport] ?: UpdateState()
        state.copy(updating = status.updating, updateFailed = status.failed)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
        TimetableUiState(date = date.value, now = clock.instant())
    )

    fun selectStation(station: TimetableStation) {
        savedState[stationKey(uiState.value.transport)] = station.id
        revision.update { it + 1 }
    }

    fun selectDate(value: LocalDate) {
        savedState["timetable_date"] = value.toEpochDay()
        date.value = value
    }

    fun update(force: Boolean = true) {
        val transport = uiState.value.transport
        if (updates.value[transport]?.updating == true) return
        updates.update { it + (transport to UpdateState(updating = true)) }
        viewModelScope.launch {
            // Opening an already-downloaded timetable checks for updates; initial downloads need the button.
            val result = resultOf {
                if (force || repository.feed(transport) != null) repository.refresh(transport, force).getOrThrow()
            }
            updates.update { it + (transport to UpdateState(failed = result.isFailure)) }
            revision.update { it + 1 }
        }
    }

    private fun stationKey(transport: TransportType) = "timetable_station_${transport.name}"
}
