package com.wedgess.luas.data.alarm

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wedgess.luas.R
import com.wedgess.luas.domain.alarm.AlarmNotifier
import com.wedgess.luas.domain.alarm.ReminderContent
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmVerification
import com.wedgess.luas.domain.navigation.ServiceNavigator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val SECONDS_PER_MINUTE = 60L
private val IRELAND_ZONE: ZoneId = ZoneId.of("Europe/Dublin")
private val CLOCK_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Shows the reminder. The notification is keyed by the alarm's own request code under a fixed tag, so posting it again
 * replaces it instead of adding a second one. A reminder that was not confirmed against live data says so.
 */
class AlarmNotificationNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
    private val navigator: ServiceNavigator
) : AlarmNotifier {

    private val manager = NotificationManagerCompat.from(context)

    override fun areNotificationsEnabled(): Boolean = manager.areNotificationsEnabled()

    @SuppressLint("MissingPermission") // Guarded by areNotificationsEnabled(), which covers the runtime permission.
    override fun notify(alarm: Alarm, content: ReminderContent): Boolean {
        if (!areNotificationsEnabled()) return false
        createChannel()
        manager.notify(NOTIFICATION_TAG, alarm.requestCode, build(alarm, content))
        return true
    }

    override fun dismiss(alarm: Alarm) = manager.cancel(NOTIFICATION_TAG, alarm.requestCode)

    private fun build(alarm: Alarm, content: ReminderContent): android.app.Notification {
        val dismiss = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_DISMISS
                data = AlarmReceiver.alarmUri(alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(alarm.destination?.let { "${alarm.stationName} --> $it" } ?: alarm.stationName)
            .setContentText(text(content))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text(content)))
            .setSmallIcon(R.drawable.ic_tram)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .addAction(R.drawable.ic_close, context.getString(R.string.action_dismiss), dismiss)
            .setDeleteIntent(dismiss)
            .build()
    }

    private fun text(content: ReminderContent): String = when (content) {
        is ReminderContent.Upcoming -> upcomingText(content)
        ReminderContent.ServiceLost -> context.getString(R.string.alarm_text_service_lost)
        is ReminderContent.Missed ->
            context.getString(R.string.alarm_text_missed, content.expectedAt?.toClockTime().orEmpty())
    }

    private fun upcomingText(content: ReminderContent.Upcoming): String {
        val expected = content.expectedAt
        return when {
            // Only a result confirmed live moments ago may be shown as a countdown.
            content.verification == AlarmVerification.LIVE_VERIFIED && expected != null -> {
                val minutes = minutesUntil(expected)
                val due = context.resources.getQuantityString(R.plurals.due_minutes, minutes, minutes)
                if (content.ambiguous) context.getString(R.string.alarm_text_ambiguous, due) else due
            }
            expected != null -> context.getString(R.string.alarm_text_unverified, expected.toClockTime())
            else -> context.getString(R.string.alarm_text_unverified_unknown)
        }
    }

    private fun minutesUntil(expected: Instant): Int {
        val seconds = Duration.between(clock.instant(), expected).seconds.coerceAtLeast(0)
        return ((seconds + SECONDS_PER_MINUTE / 2) / SECONDS_PER_MINUTE).toInt()
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(context, navigator.mainActivityClass()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.alarm_channel_name))
            .setDescription(context.getString(R.string.alarm_channel_description))
            .setVibrationEnabled(true)
            .build()
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "countdown_channel"
        const val NOTIFICATION_TAG = "luas-alarm"
    }
}

private fun Instant.toClockTime(): String = CLOCK_TIME_FORMAT.format(atZone(IRELAND_ZONE))
