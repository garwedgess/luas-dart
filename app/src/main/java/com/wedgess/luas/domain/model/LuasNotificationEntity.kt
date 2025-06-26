package com.wedgess.luas.domain.model

data class LuasNotificationEntity(
    val dueInMins: Int,
    val station: String,
    val destination: String,
    val notifyMinutesBefore: Int = 0
)
