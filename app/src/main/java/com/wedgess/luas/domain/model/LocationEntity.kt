package com.wedgess.luas.domain.model

sealed interface LocationEntity {
    val name: String
    val latitude: Double
    val longitude: Double

    data class Dart(
        override val name: String,
        override val latitude: Double,
        override val longitude: Double,
    ) : LocationEntity

    data class Luas(
        override val name: String,
        override val latitude: Double,
        override val longitude: Double,
        val line: LuasLineEntity,
    ) : LocationEntity
}
