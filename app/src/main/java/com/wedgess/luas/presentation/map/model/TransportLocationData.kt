package com.wedgess.luas.presentation.map.model

import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.domain.model.TransportType

data class TransportLocationData(
    val transportType: TransportType,
    val locations: List<LocationEntity>
)
