package com.wedgess.luas.presentation.forecast.luastab.model

import com.wedgess.luas.domain.model.DepartureDirection
import java.time.Instant

/** The alarm being set up in the dialog. [requestId] is fixed when the dialog opens so confirming twice is a no-op. */
data class LuasNotificationState(
    val requestId: String = "",
    val dueInMins: Int = 0,
    val station: String = "",
    val stationCode: String = "",
    val destination: String = "",
    val direction: DepartureDirection? = null,
    val expectedAt: Instant? = null,
    val observedAt: Instant? = null,
    val notifyMinutesBefore: Int = 0
)
