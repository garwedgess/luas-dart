package com.wedgess.luas.data.mapper

import com.wedgess.luas.data.model.LuasLineData
import com.wedgess.luas.domain.model.LuasLineEntity

fun LuasLineData.toEntity() = when (this) {
    LuasLineData.RED -> LuasLineEntity.RED
    LuasLineData.GREEN -> LuasLineEntity.GREEN
}

fun LuasLineEntity.fromEntity() = when (this) {
    LuasLineEntity.RED -> LuasLineData.RED
    LuasLineEntity.GREEN -> LuasLineData.GREEN
}
