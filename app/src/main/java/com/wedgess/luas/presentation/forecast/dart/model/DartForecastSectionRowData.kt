package com.wedgess.luas.presentation.forecast.dart.model

import com.wedgess.luas.domain.model.DepartureDirection
import java.time.LocalDate

data class DartForecastSectionRowData(
    val trainCode: String,
    val destination: String,
    val status: String? = null,
    val lastLocation: String? = null,
    val dueIn: Int,
    val late: Int,
    val expectedAt: String,
    val scheduledAt: String,
    val direction: DepartureDirection,
    val operatingDate: LocalDate? = null
)
