package com.wedgess.luas.presentation.forecast

import com.wedgess.luas.domain.model.ForcastEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.presentation.forecast.model.ForecastDialogState

interface ForecastContract {

    data class UiState(
        val stops: List<StopEntity> = emptyList(),
        val selectedStop: StopEntity = StopEntity.initial(),
        val forecast: ForcastEntity = ForcastEntity.initial(),
        val refreshProgress: Float = 0f,
        val line: LuasLineEntity = LuasLineEntity.RED,
        val dialog: ForecastDialogState = ForecastDialogState.None
    )

    sealed interface Event {
        data class OnStopSelected(val stopAbrv: StopEntity) : Event
        data object OnShowTravelUpdatesDialog : Event
        data object OnDismissTravelUpdatesDialog : Event
        data object OnRefresh : Event
    }
}
