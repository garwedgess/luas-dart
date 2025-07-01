package com.wedgess.luas.presentation.forecast.dart.extensions

import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.presentation.forecast.dart.model.DartDropdownItem

fun DartStationEntity.toDropdownItem() = DartDropdownItem(text = this.name)
