package com.wedgess.luas.presentation.forecast.luastab.extensions

import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.presentation.forecast.luastab.model.LuasDropdownItem

fun LuasStopEntity.toDropdownItem(): LuasDropdownItem {
    return LuasDropdownItem(
        text = this.name,
        isParkAndRide = this.isParkAndRide,
        isCycleAndRide = this.isCycleAndRide
    )
}
