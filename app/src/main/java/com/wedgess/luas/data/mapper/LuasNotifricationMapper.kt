package com.wedgess.luas.data.mapper

import com.wedgess.luas.data.model.LuasNotificationData
import com.wedgess.luas.domain.model.LuasNotificationEntity

fun LuasNotificationEntity.toData() = LuasNotificationData(
    dueInMins = this.dueInMins,
    notifyBeforeMins = this.notifyMinutesBefore,
    destinationName = this.destination,
    stopName = this.station
)
