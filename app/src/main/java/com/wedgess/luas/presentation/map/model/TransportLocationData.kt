package com.wedgess.luas.presentation.map.model

import com.wedgess.luas.domain.model.StationLocationEntity
import com.wedgess.luas.domain.model.TransportType

data class TransportLocationData(
    val transportType: TransportType,
    val locations: List<StationLocationEntity>
)
