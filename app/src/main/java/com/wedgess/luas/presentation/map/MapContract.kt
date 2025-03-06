package com.wedgess.luas.presentation.map

import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.domain.model.UserLocation

interface MapContract {

    data class UiState(
        val currentLocation: UserLocation,
        val greenLineLocations: List<StopEntity>,
        val redLineLocations: List<StopEntity>
    ) {
        companion object {
            fun initial() = UiState(
                currentLocation = UserLocation(0.0, 0.0),
                greenLineLocations = emptyList(),
                redLineLocations = emptyList()
            )
        }
    }
}
