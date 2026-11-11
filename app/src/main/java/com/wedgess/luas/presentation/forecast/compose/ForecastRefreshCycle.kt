package com.wedgess.luas.presentation.forecast.compose

import java.time.Instant

/** The visible station's last successful refresh drives both countdown displays. */
data class ForecastRefreshCycle(val fetchedAt: Instant, val intervalMillis: Long)
