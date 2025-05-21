package com.wedgess.luas.presentation.forecast.tab.extensions

import com.wedgess.luas.domain.model.NotificationEntity
import com.wedgess.luas.presentation.forecast.tab.model.NotificationState
import java.util.concurrent.TimeUnit

fun NotificationState.toEntity() = NotificationEntity(
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
