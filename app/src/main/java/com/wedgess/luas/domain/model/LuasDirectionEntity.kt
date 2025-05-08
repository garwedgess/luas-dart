package com.wedgess.luas.domain.model

enum class LuasDirectionEntity(val key: String) {
    INBOUND("inbound"),
    OUTBOUND("outbound");

    companion object {
        operator fun get(key: String) = requireNotNull(LuasDirectionEntity.entries.find { it.key == key })
    }
}
