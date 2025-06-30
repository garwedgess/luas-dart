package com.wedgess.luas.domain.model

enum class TransportType(val type: Int) {
    LUAS(0),
    DART(1);

    companion object {
        operator fun get(type: Int) = requireNotNull(
            entries.find { it.type == type }
        ) {
            "Unable to find ${TransportType::class.simpleName} for type: $type, possible values ${
                entries.joinToString(
                    separator = ",",
                    transform = { "${it.name}:${it.type}" }
                )
            }"
        }
    }
}
