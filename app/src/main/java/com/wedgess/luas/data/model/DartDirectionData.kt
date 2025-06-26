package com.wedgess.luas.data.model

enum class DartDirectionData(val key: String) {
    NORTHBOUND("Northbound"),
    SOUTHBOUND("Southbound"),
    UNKNOWN("Unknown"); // To Grand Canal Dock

    companion object {
        operator fun get(key: String) = requireNotNull(entries.find { it.key == key })
    }
}
