package com.wedgess.luas.domain.model

data class ForcastEntity(
    val createdAt: String,
    val stop: String,
    val stopAbv: String,
    val message: String,
    val inboundTrams: List<TramEntity>,
    val outboundTrams: List<TramEntity>
) {
    data class TramEntity(
        val dueMins: Int,
        val destination: String
    )

    companion object {
        fun initial() = ForcastEntity(
            createdAt = "",
            stop = "",
            stopAbv = "",
            message = "",
            inboundTrams = emptyList(),
            outboundTrams = emptyList()
        )
    }
}
