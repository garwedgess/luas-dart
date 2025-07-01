package com.wedgess.luas.presentation.forecast.model

import com.wedgess.luas.presentation.model.DropdownItem

interface TransportDropdownRenderer<T> {
    fun render(option: T): DropdownItem
}
