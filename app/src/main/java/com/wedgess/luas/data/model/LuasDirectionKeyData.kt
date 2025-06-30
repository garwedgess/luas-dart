package com.wedgess.luas.data.model

enum class LuasDirectionKeyData(val key: String) {
    INBOUND("inbound"),
    OUTBOUND("outbound");

    companion object {
        operator fun get(key: String) =
            requireNotNull(LuasDirectionKeyData.entries.find { it.key == key }) {
                "Could not find key: $key for ${LuasDirectionKeyData::class.java::getSimpleName}, " +
                    "available values are ${LuasDirectionKeyData.entries.joinToString(", ")
                }"
            }
    }
}
