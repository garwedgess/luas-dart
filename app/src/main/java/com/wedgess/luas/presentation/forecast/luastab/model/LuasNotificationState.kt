package com.wedgess.luas.presentation.forecast.luastab.model

data class LuasNotificationState(
    val dueInMins: Int = 0,
    val station: String = "",
    val stationCode: String = "",
    val destination: String = "",
    val notifyMinutesBefore: Int = 0
)
