package com.wedgess.luas.presentation.forecast.luastab.extensions

import com.wedgess.luas.domain.model.LuasNotificationEntity
import com.wedgess.luas.presentation.forecast.luastab.model.NotificationState
import java.util.concurrent.TimeUnit

fun NotificationState.toEntity() = LuasNotificationEntity(
    dueInMins = dueInMins,
    station = station,
    notifyMinutesBefore = notifyMinutesBefore,
    destination = destination
)

fun NotificationState.triggerTimeSeconds(elapsedTimeSinceAction: Long): Long {
    return TimeUnit.MINUTES.toSeconds((dueInMins - notifyMinutesBefore).toLong()).run {
        val elapsedTimeMillis = System.currentTimeMillis() - elapsedTimeSinceAction
        this - (elapsedTimeMillis / TimeUnit.SECONDS.toMillis(1))
    }
}
