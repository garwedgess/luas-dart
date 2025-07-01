package com.wedgess.luas.presentation.forecast.dart.model

import com.wedgess.luas.presentation.model.DropdownItem

data class DartDropdownItem(
    override val text: String
) : DropdownItem {
    companion object {
        val default = DartDropdownItem(text = "")
    }
}
