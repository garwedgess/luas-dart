package com.wedgess.luas.presentation.forecast

import com.wedgess.luas.domain.model.ForcastEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.StopEntity

interface ForecastContract {

    data class UiState(
        val stops: List<StopEntity> = emptyList(),
        val selectedStop: StopEntity = StopEntity.initial(),
        val forecast: ForcastEntity = ForcastEntity.initial(),
        val line: LuasLineEntity = LuasLineEntity.RED
    )

    sealed interface Event {
        data class OnStopSelected(val stopAbrv: StopEntity) : Event
    }
}