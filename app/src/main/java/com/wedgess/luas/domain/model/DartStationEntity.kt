package com.wedgess.luas.domain.model

data class DartStationEntity(
    val id: Long,
    val name: String,
    val code: String,
    val alias: String? = null,
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        fun initial(id: Long = 0L) = DartStationEntity(
            id = id,
            name = "",
            code = "",
            alias = null,
            latitude = 0.0,
            longitude = 0.0
        )
    }
}
