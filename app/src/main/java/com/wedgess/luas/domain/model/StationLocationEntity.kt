package com.wedgess.luas.domain.model

sealed interface StationLocationEntity {
    val name: String
    val latitude: Double
    val longitude: Double

    data class DartStationLocationEntity(
        override val name: String,
        override val latitude: Double,
        override val longitude: Double,
    ) : StationLocationEntity

    data class LuasStationLocationEntity(
        override val name: String,
        override val latitude: Double,
        override val longitude: Double,
        val line: LuasLineEntity,
    ) : StationLocationEntity
}


