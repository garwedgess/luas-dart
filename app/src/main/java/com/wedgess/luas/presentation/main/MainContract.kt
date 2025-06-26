package com.wedgess.luas.presentation.main

import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.main.model.TopAppBarState

interface MainContract {

    data class UiState(
        val appBarState: TopAppBarState = TopAppBarState(title = "Forecast"),
        val transportType: TransportType = TransportType.LUAS,
        val refreshProgress: Float = 0f
    )

    sealed interface Event {
        data class OnUpdateAppBarState(val appbarState: TopAppBarState) : Event
        data class OnTransportTypeChange(val transportType: TransportType) : Event
        data class OnUpdateRefreshProgress(val progress: Float) : Event
    }
}
