package com.wedgess.luas.data.alarm

import android.content.Intent
import android.net.Uri

/** Only the durable alarm identity crosses the notification boundary. */
object ReminderIntents {
    fun uri(alarmId: String): Uri = Uri.Builder().scheme("luasreminder").authority("alarm").appendPath(alarmId).build()

    fun alarmId(intent: Intent): String? = intent.data?.takeIf {
        intent.action == Intent.ACTION_VIEW && it.scheme == "luasreminder" && it.authority == "alarm" &&
            it.pathSegments.size == 1
    }?.lastPathSegment?.takeIf { it.isNotBlank() }
}
