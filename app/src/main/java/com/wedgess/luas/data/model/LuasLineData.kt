package com.wedgess.luas.data.model

enum class LuasLineData(val key: Long) {
    RED(1),
    GREEN(2);

    companion object {
        operator fun get(key: Long) = requireNotNull(LuasLineData.entries.find { it.key == key }) {
            "Could not find key: $key for ${LuasLineData::class.java::getSimpleName}, available values are ${
                LuasLineData.entries.joinToString(
                    ", "
                )
            }"
        }
    }
}