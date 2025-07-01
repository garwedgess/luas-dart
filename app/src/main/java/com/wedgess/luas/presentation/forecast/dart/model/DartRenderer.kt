package com.wedgess.luas.presentation.forecast.dart.model

import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.presentation.forecast.model.TransportDropdownRenderer
import com.wedgess.luas.presentation.model.DropdownItem

object DartRenderer : TransportDropdownRenderer<DartStationEntity> {
    override fun render(option: DartStationEntity): DropdownItem {
        return DropdownItem(
            text = option.name
        )
    }
}
