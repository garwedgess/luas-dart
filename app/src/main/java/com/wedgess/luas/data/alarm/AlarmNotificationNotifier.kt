package com.wedgess.luas.data.alarm

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.wedgess.luas.domain.alarm.AlarmNotifier
import com.wedgess.luas.domain.alarm.ReminderContent
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmVerification
import com.wedgess.luas.domain.navigation.ServiceNavigator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns visible reminders under their request codes. Updates and dismissal share a lock so a late response cannot
 * resurrect a dismissed notification. The foreground service uses the same untagged notification ID.
 */
@Singleton
@Suppress("TooManyFunctions") // Notification lifecycle operations share one lock to make dismissal final.
class AlarmNotificationNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
    navigator: ServiceNavigator,
    private val liveNotifications: LiveNotificationController
) : AlarmNotifier {

    private val factory = ReminderNotificationFactory(context, clock, navigator)
    private val manager = NotificationManagerCompat.from(context)
    private val lock = Any()
    private val visible = mutableMapOf<String, VisibleReminder>()
    private val dismissed = mutableSetOf<String>()

    override fun areNotificationsEnabled(): Boolean = manager.areNotificationsEnabled()

    @SuppressLint("MissingPermission") // Checked before posting and again by Android.
    override fun notify(alarm: Alarm, content: ReminderContent): Boolean {
        if (!areNotificationsEnabled()) return false
        factory.createChannel()
        val checked = if (
            content is ReminderContent.Upcoming && content.verification == AlarmVerification.LIVE_VERIFIED
        ) {
            content.copy(lastCheckedAt = content.lastCheckedAt ?: clock.instant())
        } else {
            content
        }
        val tracking = checked is ReminderContent.Upcoming && checked.expectedAt != null
        synchronized(lock) {
            if (alarm.id in dismissed) return false
            visible[alarm.id] = VisibleReminder(alarm, checked, tracking)
            // Migrate previously delivered tagged notifications without duplicating them.
            manager.cancel(LEGACY_NOTIFICATION_TAG, alarm.requestCode)
            manager.notify(alarm.requestCode, factory.build(alarm, checked, tracking))
        }
        if (tracking && !liveNotifications.start(alarm.id)) {
            finishTracking(
                alarm.id,
                (checked as ReminderContent.Upcoming).copy(
                    verification = AlarmVerification.UNVERIFIED,
                    backgroundRestricted = true
                )
            )
        }
        return true
    }

    override fun dismiss(alarm: Alarm) {
        dismiss(alarm.id, alarm.requestCode)
    }

    /** Called directly by the notification receiver, before any database lookup or service callback. */
    fun dismiss(id: String, requestCode: Int) {
        synchronized(lock) {
            dismissed += id
            visible.remove(id)
            manager.cancel(requestCode)
            manager.cancel(LEGACY_NOTIFICATION_TAG, requestCode)
        }
        liveNotifications.stop(id)
    }

    internal fun remove(alarm: Alarm) = synchronized(lock) {
        dismissed += alarm.id
        visible.remove(alarm.id)
        manager.cancel(alarm.requestCode)
        manager.cancel(LEGACY_NOTIFICATION_TAG, alarm.requestCode)
    }

    internal fun snapshot(id: String): VisibleReminder? = synchronized(lock) {
        visible[id]?.takeIf { it.tracking }
    }

    internal fun visibleIds(): Set<String> = manager.activeNotifications.filter { it.tag == null }
        .mapNotNull { it.notification.extras.getString(EXTRA_NOTIFICATION_ALARM_ID) }.toSet()

    internal fun isVisible(alarm: Alarm): Boolean = alarm.id in visibleIds()

    /** Promotion must not use a snapshot captured before the user dismissed the reminder. */
    internal fun promote(id: String, start: (Int, Notification) -> Unit): Boolean = synchronized(lock) {
        val current = visible[id]?.takeIf { it.tracking } ?: return false
        start(current.alarm.requestCode, factory.build(current.alarm, current.content, tracking = true))
        true
    }

    @SuppressLint("MissingPermission") // Permission can change while tracking; stop instead of re-posting then.
    internal fun update(id: String, content: ReminderContent): Boolean = synchronized(lock) {
        val current = visible[id] ?: return false
        if (!areNotificationsEnabled() || !isVisible(current.alarm)) return false
        visible[id] = current.copy(content = content)
        manager.notify(current.alarm.requestCode, factory.build(current.alarm, content, tracking = true))
        true
    }

    @SuppressLint("MissingPermission")
    internal fun finishTracking(id: String, finalContent: ReminderContent? = null) = synchronized(lock) {
        val current = visible.remove(id) ?: return
        if (!areNotificationsEnabled() || !isVisible(current.alarm)) return
        val content = finalContent ?: (current.content as? ReminderContent.Upcoming)?.copy(
            verification = AlarmVerification.UNVERIFIED
        ) ?: current.content
        manager.notify(current.alarm.requestCode, factory.build(current.alarm, content, tracking = false))
    }

    @SuppressLint("MissingPermission") // Restores old reminders as snapshots, never as a new live session.
    fun stopOrphanedTracking() = synchronized(lock) {
        if (!areNotificationsEnabled()) return
        manager.activeNotifications.filter {
            it.notification.extras.getString(EXTRA_NOTIFICATION_ALARM_ID) !in dismissed &&
                (
                    it.tag == LEGACY_NOTIFICATION_TAG ||
                        (
                            it.notification.extras.getBoolean(EXTRA_TRACKING) &&
                                it.notification.extras.getString(EXTRA_NOTIFICATION_ALARM_ID) !in visible
                            )
                    )
        }.forEach {
            manager.notify(it.tag, it.id, factory.unavailable(it.notification, it.tag == LEGACY_NOTIFICATION_TAG))
        }
    }

    private companion object {
        const val LEGACY_NOTIFICATION_TAG = "luas-alarm"
    }
}

internal data class VisibleReminder(val alarm: Alarm, val content: ReminderContent, val tracking: Boolean)
