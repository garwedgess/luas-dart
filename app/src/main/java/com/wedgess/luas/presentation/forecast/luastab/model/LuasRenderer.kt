package com.wedgess.luas.presentation.forecast.luastab.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.PedalBike
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.presentation.forecast.model.TransportDropdownRenderer
import com.wedgess.luas.presentation.model.DropdownItem

object LuasRenderer : TransportDropdownRenderer<LuasStopEntity> {
    override fun render(option: LuasStopEntity): DropdownItem {
        return DropdownItem(
            text = option.name,
            icons = buildList {
                if (option.isParkAndRide) add(Icons.Default.LocalParking)
                if (option.isCycleAndRide) add(Icons.Default.PedalBike)
            }
        )
    }
}
