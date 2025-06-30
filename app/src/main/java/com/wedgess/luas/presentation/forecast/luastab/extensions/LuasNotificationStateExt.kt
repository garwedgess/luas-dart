package com.wedgess.luas.presentation.forecast.luastab.extensions

import com.wedgess.luas.domain.model.LuasNotificationEntity
import com.wedgess.luas.presentation.forecast.luastab.model.LuasNotificationState
import java.util.concurrent.TimeUnit

fun LuasNotificationState.toEntity() = LuasNotificationEntity(
    dueInMins = dueInMins,
    station = station,
    notifyMinutesBefore = notifyMinutesBefore,
    destination = destination
)

fun LuasNotificationState.triggerTimeSeconds(elapsedTimeSinceAction: Long): Long {
    return TimeUnit.MINUTES.toSeconds((dueInMins - notifyMinutesBefore).toLong()).run {
        val elapsedTimeMillis = System.currentTimeMillis() - elapsedTimeSinceAction
        this - (elapsedTimeMillis / TimeUnit.SECONDS.toMillis(1))
    }
}
