package com.wedgess.luas.data.model

enum class DirectionKeyData(val key: String) {
    INBOUND("inbound"),
    OUTBOUND("outbound");

    companion object {
        operator fun get(key: String) =
            requireNotNull(DirectionKeyData.entries.find { it.key == key }) {
                "Could not find key: $key for ${DirectionKeyData::class.java::getSimpleName}, available values are ${
                    DirectionKeyData.entries.joinToString(
                        ", "
                    )
                }"
            }
    }
}