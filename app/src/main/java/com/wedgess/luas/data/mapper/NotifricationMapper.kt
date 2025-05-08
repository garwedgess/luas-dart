package com.wedgess.luas.data.mapper

import com.wedgess.luas.data.model.NotificationData
import com.wedgess.luas.domain.model.NotificationEntity

fun NotificationEntity.toData() = NotificationData(
    dueInMins = this.dueInMins,
    notifyBeforeMins = this.notifyMinutesBefore,
    destinationName = this.destination,
    stopName = this.station,
)
