package com.wedgess.luas.presentation.forecast.tab.model

data class NotificationState(
    val dueInMins: Int = 0,
    val station: String = "",
    val destination: String = "",
    val notifyMinutesBefore: Int = 0,
)
