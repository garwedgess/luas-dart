package com.wedgess.luas.presentation.forecast.extensions

import com.wedgess.luas.domain.model.NotificationEntity
import com.wedgess.luas.presentation.forecast.model.NotificationState

fun NotificationState.toEntity() = NotificationEntity(
    dueInMins = dueInMins,
    station = station,
    notifyMinutesBefore = notifyMinutesBefore,
    destination = destination,
)

fun NotificationState.triggerTimeSeconds(elapsedTimeSinceAction: Long): Long {
    return ((dueInMins - notifyMinutesBefore) * 60).run {
        val elapsedTimeMillis = System.currentTimeMillis() - elapsedTimeSinceAction
        this - (elapsedTimeMillis / 1000)
    }
}
