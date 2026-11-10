package com.wedgess.luas.domain.model

sealed interface LocationEntity {
    val stationCode: String
    val name: String
    val latitude: Double
    val longitude: Double

    data class Dart(
        override val name: String,
        override val latitude: Double,
        override val longitude: Double,
        override val stationCode: String = ""
    ) : LocationEntity

    data class Luas(
        override val name: String,
        override val latitude: Double,
        override val longitude: Double,
        val line: LuasLineEntity,
        override val stationCode: String = ""
    ) : LocationEntity
}
