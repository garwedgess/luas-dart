package com.wedgess.luas.presentation.map.model

import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.presentation.model.UiResult

data class MapStationState(
    val station: LocationEntity,
    val result: UiResult<Departures> = UiResult.Loading,
    val isStale: Boolean = false
)
