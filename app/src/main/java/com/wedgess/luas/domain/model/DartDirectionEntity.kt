package com.wedgess.luas.domain.model

enum class DartDirectionEntity(val id: Long, val key: String) {
    NORTHBOUND(1L, "Northbound"),
    SOUTHBOUND(2L, "Southbound"),
    UNKNOWN(3L, "Unknown");

    companion object {
        operator fun get(key: String) = requireNotNull(entries.find { it.key == key })
    }
}
