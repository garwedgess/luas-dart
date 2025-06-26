package com.wedgess.luas.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.wedgess.luas.domain.usecase.FetchSelectedTransportTypeUseCase
import com.wedgess.luas.domain.usecase.UpdateSelectedTransportTypeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    fetchSelectedTransportTypeUseCase: FetchSelectedTransportTypeUseCase,
    private val updateSelectedTransportTypeUseCase: UpdateSelectedTransportTypeUseCase,
) : ViewModel() {

    private val initialState = MainContract.UiState()

    private val _uiState = MutableStateFlow(initialState)

    private var transportType = initialState.transportType

    val uiState = _uiState.combine(fetchSelectedTransportTypeUseCase()) { state, transportType ->
        Timber.d("GARETH TransportType changed in flow: $transportType")
        this.transportType = transportType
        state.copy(
            transportType = transportType,
            appBarState = state.appBarState.copy(transportType = transportType)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState)

    @OptIn(ExperimentalPermissionsApi::class)
    fun onEvent(event: MainContract.Event) {
        when (event) {
            is MainContract.Event.OnTransportTypeChange -> {
                viewModelScope.launch {
                    updateSelectedTransportTypeUseCase.invoke(event.transportType)
                }
            }

            is MainContract.Event.OnUpdateAppBarState -> {
                Timber.d("GARETH App bar state updated")
                _uiState.update {
                    it.copy(appBarState = event.appbarState.copy(transportType = transportType))
                }
            }

            is MainContract.Event.OnUpdateRefreshProgress -> {
                Timber.d("GARETH Refresh progress")
                _uiState.update {
                    it.copy(refreshProgress = event.progress)
                }
            }
        }
    }
}
