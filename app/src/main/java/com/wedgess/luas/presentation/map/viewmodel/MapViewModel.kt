package com.wedgess.luas.presentation.map.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.UserLocation
import com.wedgess.luas.domain.usecase.FetchAllStopsUseCase
import com.wedgess.luas.domain.usecase.FetchCurrentLocationUseCase
import com.wedgess.luas.presentation.map.MapContract
import com.wedgess.luas.presentation.model.UiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    fetchCurrentLocationUseCase: FetchCurrentLocationUseCase,
    fetchAllStopsUseCase: FetchAllStopsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapContract.UiState.initial())

    val uiResult = combine(
        _uiState,
        fetchCurrentLocationUseCase(),
        fetchAllStopsUseCase()
    ) { state, currentLocation, allStopsResult ->
        val allStops = allStopsResult.getOrDefault(emptyList())
        UiResult.Success(
            state.copy(
                currentLocation = currentLocation ?: UserLocation(0.0, 0.0),
                greenLineLocations = allStops.filter { it.line == LuasLineEntity.GREEN },
                redLineLocations = allStops.filter { it.line == LuasLineEntity.RED }
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiResult.Loading)
}
