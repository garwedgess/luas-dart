package com.wedgess.luas.domain.model

enum class LuasLineEntity(val key: Long) {
    RED(1),
    GREEN(2);

    companion object {
        operator fun get(key: Long) = requireNotNull(LuasLineEntity.entries.find { it.key == key }) {
            "Could not find key: $key for ${LuasLineEntity::class.java::getSimpleName}, available values are ${
                LuasLineEntity.entries.joinToString(
                    ", "
                )
            }"
        }
    }
}
