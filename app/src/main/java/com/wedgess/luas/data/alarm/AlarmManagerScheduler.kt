package com.wedgess.luas.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.wedgess.luas.domain.alarm.AlarmScheduler
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmRegistration
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

/**
 * Registers one AlarmManager wake-up per alarm. The PendingIntent is identified by the alarm id (carried in the intent
 * data), so alarms never share or overwrite each other's registration, and registering again simply replaces it.
 * Wall-clock (RTC) times are used because the stored reminder time is an absolute instant that survives a reboot.
 */
class AlarmManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmScheduler {

    private val alarmManager: AlarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    override fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    override fun register(alarm: Alarm, triggerAt: Instant): AlarmRegistration {
        val pendingIntent = requireNotNull(pendingIntent(alarm.id, create = true))
        val triggerAtMillis = triggerAt.toEpochMilli()
        if (canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                return AlarmRegistration.EXACT
            } catch (_: SecurityException) {
                // The permission was revoked between the check and the call: use the inexact fallback below.
            }
        }
        // Inexact: may be delayed by the system. The coordinator copes with late delivery.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        return AlarmRegistration.INEXACT
    }

    override fun cancel(alarmId: String) {
        pendingIntent(alarmId, create = false)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    override fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private fun pendingIntent(alarmId: String, create: Boolean): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_DUE
            data = AlarmReceiver.alarmUri(alarmId)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }
}
