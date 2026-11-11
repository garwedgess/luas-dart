package com.wedgess.luas.data.alarm

import android.annotation.SuppressLint
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
            visible[alarm.id] = VisibleReminder(alarm, checked, tracking)
            // Migrate previously delivered tagged notifications without duplicating them.
            manager.cancel(LEGACY_NOTIFICATION_TAG, alarm.requestCode)
            manager.notify(alarm.requestCode, factory.build(alarm, checked, tracking))
        }
        if (tracking && !liveNotifications.start(alarm.id)) finishTracking(alarm.id)
        return true
    }

    override fun dismiss(alarm: Alarm) {
        remove(alarm)
        liveNotifications.stop(alarm.id)
    }

    internal fun remove(alarm: Alarm) = synchronized(lock) {
        visible.remove(alarm.id)
        manager.cancel(alarm.requestCode)
        manager.cancel(LEGACY_NOTIFICATION_TAG, alarm.requestCode)
    }

    internal fun snapshot(id: String): VisibleReminder? = synchronized(lock) {
        visible[id]?.takeIf { it.tracking }
    }

    internal fun isVisible(alarm: Alarm): Boolean = manager.activeNotifications.any {
        it.id == alarm.requestCode && it.tag == null && it.notification.extras.getString(
            EXTRA_NOTIFICATION_ALARM_ID
        ) == alarm.id
    }

    internal fun foregroundNotification(snapshot: VisibleReminder): android.app.Notification =
        factory.build(snapshot.alarm, snapshot.content, tracking = true)

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
            it.tag == LEGACY_NOTIFICATION_TAG ||
                (
                    it.notification.extras.getBoolean(EXTRA_TRACKING) &&
                        it.notification.extras.getString(EXTRA_NOTIFICATION_ALARM_ID) !in visible
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
