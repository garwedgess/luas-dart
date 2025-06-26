package com.wedgess.luas.data.model

enum class DartLocationTypeData(val key: String) {
    ORIGIN("O"),
    DESTINATION("D"),
    STOP("S");

    companion object {
        operator fun get(key: String) = requireNotNull(entries.find { it.key == key })
    }
}
