package com.wedgess.luas.data.alarm

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/** Starting can be refused for an inexact background alarm; the reminder must remain usable in that case. */
interface LiveNotificationController {
    fun start(alarmId: String): Boolean
    fun stop(alarmId: String)
}

class AndroidLiveNotificationController @Inject constructor(
    @ApplicationContext private val context: Context
) : LiveNotificationController {
    override fun start(alarmId: String): Boolean = try {
        ContextCompat.startForegroundService(
            context,
            Intent(context, LiveReminderService::class.java).putExtra(LiveReminderService.EXTRA_ALARM_ID, alarmId)
        )
        true
    } catch (e: IllegalStateException) {
        Timber.w(e, "Android did not allow live reminder tracking")
        false
    } catch (e: SecurityException) {
        Timber.w(e, "Android denied live reminder tracking")
        false
    }

    override fun stop(alarmId: String) {
        // This reaches a running service only; dismissing an old notification must never start one.
        context.sendBroadcast(
            Intent(LiveReminderService.ACTION_DISMISS)
                .setPackage(context.packageName)
                .putExtra(LiveReminderService.EXTRA_ALARM_ID, alarmId)
        )
    }
}
