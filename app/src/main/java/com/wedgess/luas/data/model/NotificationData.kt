package com.wedgess.luas.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NotificationData(
    val destinationName: String = "",
    val stopName: String = "",
    val dueInMins: Int = 0,
    val notifyBeforeMins: Int = 0
) : Parcelable {
    val notificationTitle: String get() = "$stopName --> $destinationName"
}
