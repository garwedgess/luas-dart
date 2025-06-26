package com.wedgess.luas.domain.model

enum class DartLocationTypeEntity(val key: String) {
    ORIGIN("O"),
    DESTINATION("D"),
    STOP("S");

    companion object {
        operator fun get(key: String) = requireNotNull(entries.find { it.key == key })
    }
}
