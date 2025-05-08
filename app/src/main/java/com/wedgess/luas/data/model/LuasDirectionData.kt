package com.wedgess.luas.data.model

enum class LuasDirectionData(val key: String) {
    INBOUND("inbound"),
    OUTBOUND("outbound");

    companion object {
        operator fun get(key: String) = requireNotNull(LuasDirectionData.entries.find { it.key == key })
    }
}
