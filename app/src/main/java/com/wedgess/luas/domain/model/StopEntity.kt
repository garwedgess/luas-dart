package com.wedgess.luas.domain.model

import java.util.UUID

data class StopEntity(
    val id: UUID,
    val abbreviation: String,
    val isParkAndRide: Boolean,
    val isCycleAndRide: Boolean,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val line: LuasLineEntity
) {
    companion object {
        fun initial() = StopEntity(
            id = UUID.randomUUID(),
            abbreviation = "",
            isParkAndRide = false,
            isCycleAndRide = false,
            latitude = 0.0,
            longitude = 0.0,
            name = "",
            line = LuasLineEntity.GREEN
        )
    }
}
