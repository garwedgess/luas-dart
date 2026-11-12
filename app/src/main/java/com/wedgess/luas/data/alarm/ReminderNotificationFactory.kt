package com.wedgess.luas.data.alarm

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wedgess.luas.R
import com.wedgess.luas.domain.alarm.MissedGrace
import com.wedgess.luas.domain.alarm.ReminderContent
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmVerification
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.navigation.ServiceNavigator
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Builds content and tap/dismiss behavior independently of the visible-session lifecycle. */
internal class ReminderNotificationFactory(
    private val context: Context,
    private val clock: Clock,
    private val navigator: ServiceNavigator
) {
    fun build(alarm: Alarm, content: ReminderContent, tracking: Boolean): android.app.Notification {
        val dismiss = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_DISMISS
                data = AlarmReceiver.alarmUri(alarm.id)
                putExtra(AlarmReceiver.EXTRA_REQUEST_CODE, alarm.requestCode)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val upcoming = content as? ReminderContent.Upcoming
        val ticking = tracking && upcoming?.verification == AlarmVerification.LIVE_VERIFIED &&
            upcoming.expectedAt?.isAfter(clock.instant()) == true
        val extras = Bundle().apply {
            putString(EXTRA_NOTIFICATION_ALARM_ID, alarm.id)
            putBoolean(EXTRA_TRACKING, tracking)
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(alarm.destination?.let { "${alarm.stationName} --> $it" } ?: alarm.stationName)
            .setContentText(text(content, tracking))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text(content, tracking)))
            .setSmallIcon(if (alarm.transport == TransportType.DART) R.drawable.ic_train else R.drawable.ic_tram)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(tracking)
            .setAutoCancel(!tracking)
            .setWhen(upcoming?.expectedAt?.toEpochMilli() ?: clock.millis())
            .setShowWhen(ticking)
            .setUsesChronometer(ticking)
            .setChronometerCountDown(ticking)
            .setSubText(trackingStatus(upcoming, tracking))
            .setTimeoutAfter(if (tracking && ticking) LIVE_NOTIFICATION_TIMEOUT else STATIC_NOTIFICATION_TIMEOUT)
            .addExtras(extras)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent(alarm))
            .setDeleteIntent(dismiss)
        if (!tracking && upcoming?.expectedAt?.plus(MissedGrace)?.isAfter(clock.instant()) == true) {
            builder.addAction(
                R.drawable.ic_refresh,
                context.getString(R.string.alarm_resume_updates),
                resumeIntent(alarm.id, upcoming.expectedAt)
            )
        }
        return builder.addAction(R.drawable.ic_close, context.getString(R.string.action_dismiss), dismiss).build()
    }

    private fun text(content: ReminderContent, tracking: Boolean): String = when (content) {
        is ReminderContent.Upcoming -> upcomingText(content, tracking)
        is ReminderContent.TrackingFinished -> context.getString(
            R.string.alarm_tracking_finished,
            content.expectedAt?.toClockTime().orEmpty()
        )
        ReminderContent.ServiceLost -> context.getString(R.string.alarm_text_service_lost)
        is ReminderContent.Missed ->
            context.getString(R.string.alarm_text_missed, content.expectedAt?.toClockTime().orEmpty())
    }

    private fun upcomingText(content: ReminderContent.Upcoming, tracking: Boolean): String {
        val expected = content.expectedAt
        return when {
            // Only a result confirmed live moments ago may be shown as a countdown.
            content.verification == AlarmVerification.LIVE_VERIFIED && expected != null -> {
                val minutes = minutesUntil(expected)
                val due = context.resources.getQuantityString(R.plurals.due_minutes, minutes, minutes)
                if (content.ambiguous) context.getString(R.string.alarm_text_ambiguous, due) else due
            }
            content.backgroundRestricted && expected != null ->
                context.getString(R.string.alarm_background_blocked, expected.toClockTime())
            tracking && expected != null -> context.getString(R.string.alarm_updates_retrying, expected.toClockTime())
            expected != null && content.lastCheckedAt != null ->
                context.getString(R.string.alarm_live_unavailable, expected.toClockTime())
            expected != null -> context.getString(R.string.alarm_text_unverified, expected.toClockTime())
            else -> context.getString(R.string.alarm_text_unverified_unknown)
        }
    }

    private fun resumeIntent(alarmId: String, expectedAt: Instant): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_RESUME
            data = AlarmReceiver.alarmUri(alarmId)
            putExtra(AlarmReceiver.EXTRA_EXPECTED_AT, expectedAt.toEpochMilli())
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun trackingStatus(content: ReminderContent.Upcoming?, tracking: Boolean): String? = when {
        !tracking -> null
        content?.verification != AlarmVerification.LIVE_VERIFIED -> context.getString(R.string.alarm_updates_paused)
        else -> context.getString(R.string.alarm_updates_live)
    }

    private fun minutesUntil(expected: Instant): Int {
        val seconds = Duration.between(clock.instant(), expected).seconds.coerceAtLeast(0)
        return ((seconds + SECONDS_PER_MINUTE / 2) / SECONDS_PER_MINUTE).toInt()
    }

    private fun contentIntent(alarm: Alarm): PendingIntent {
        val intent = Intent(context, navigator.mainActivityClass()).apply {
            action = Intent.ACTION_VIEW
            data = ReminderIntents.uri(alarm.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun createChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.alarm_channel_name))
            .setDescription(context.getString(R.string.alarm_channel_description))
            .setVibrationEnabled(true)
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun unavailable(notification: Notification, legacy: Boolean): Notification {
        val message = if (legacy) {
            context.getString(R.string.alarm_old_snapshot)
        } else {
            context.getString(R.string.alarm_live_unavailable, Instant.ofEpochMilli(notification.`when`).toClockTime())
        }
        val extras = Bundle().apply { putBoolean(EXTRA_TRACKING, false) }
        val builder = Notification.Builder.recoverBuilder(context, notification)
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setSubText(null)
            .setShowWhen(false)
            .setUsesChronometer(false)
            .setChronometerCountDown(false)
            .setOngoing(false)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .addExtras(extras)
        val alarmId = notification.extras.getString(EXTRA_NOTIFICATION_ALARM_ID)
        val expectedAt = Instant.ofEpochMilli(notification.`when`)
        if (!legacy && alarmId != null && expectedAt.plus(MissedGrace).isAfter(clock.instant())) {
            builder.addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_refresh),
                    context.getString(R.string.alarm_resume_updates),
                    resumeIntent(alarmId, expectedAt)
                ).build()
            )
        }
        return builder.build()
    }
}

internal const val EXTRA_NOTIFICATION_ALARM_ID = "com.wedgess.luas.NOTIFICATION_ALARM_ID"
internal const val EXTRA_TRACKING = "com.wedgess.luas.NOTIFICATION_TRACKING"
private const val CHANNEL_ID = "countdown_channel"
private const val SECONDS_PER_MINUTE = 60L
private const val LIVE_NOTIFICATION_TIMEOUT = 70_000L
private const val STATIC_NOTIFICATION_TIMEOUT = 30 * 60_000L
private val IRELAND_ZONE: ZoneId = ZoneId.of("Europe/Dublin")
private val CLOCK_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun Instant.toClockTime(): String = CLOCK_TIME_FORMAT.format(atZone(IRELAND_ZONE))
