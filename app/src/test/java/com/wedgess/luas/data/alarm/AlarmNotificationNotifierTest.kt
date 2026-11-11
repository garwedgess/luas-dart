package com.wedgess.luas.data.alarm

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.wedgess.luas.MainActivity
import com.wedgess.luas.domain.alarm.ReminderContent
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.AlarmVerification
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.navigation.ServiceNavigator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class AlarmNotificationNotifierTest {

    private lateinit var context: Context
    private lateinit var manager: NotificationManager
    private lateinit var notifier: AlarmNotificationNotifier
    private val liveNotifications = mockk<LiveNotificationController>(relaxed = true)
    private val now = Instant.parse("2026-11-09T22:00:00Z")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val navigator = mockk<ServiceNavigator> { every { mainActivityClass() } returns MainActivity::class.java }
        every { liveNotifications.start(any()) } returns true
        notifier = AlarmNotificationNotifier(context, Clock.fixed(now, ZoneOffset.UTC), navigator, liveNotifications)
    }

    private fun alarm(id: String = "a", code: Int = 1) = Alarm(
        id = id,
        requestCode = code,
        kind = AlarmKind.SPECIFIC_DEPARTURE,
        state = AlarmState.ACTIVE,
        transport = TransportType.LUAS,
        stationCode = "STI",
        stationName = "Stillorgan",
        destination = "Broombridge",
        notifyMinutesBefore = 3,
        reminderAt = now,
        createdAt = now,
        updatedAt = now
    )

    private fun notification(code: Int): Notification? = shadowOf(manager).getNotification(code)

    private fun text(code: Int) = notification(code)!!.extras.getCharSequence(Notification.EXTRA_TEXT).toString()

    @Test
    fun `a live-verified reminder shows the minutes remaining`() {
        notifier.notify(alarm(), ReminderContent.Upcoming(now.plusSeconds(180), AlarmVerification.LIVE_VERIFIED))

        assertEquals("Due in 3 minutes", text(1))
        assertEquals(
            "Stillorgan --> Broombridge",
            notification(1)!!.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
        )
    }

    @Test
    fun `a reminder that was not checked live never shows a countdown and says so`() {
        notifier.notify(alarm(), ReminderContent.Upcoming(now.plusSeconds(180), AlarmVerification.UNVERIFIED))

        val shown = text(1)
        assertTrue(shown, shown.contains("Not checked against live times"))
        assertFalse(shown.contains("Due in"))
        assertTrue(shown.contains("22:03")) // Dublin is on GMT in November
    }

    @Test
    fun `an unverified reminder with no known time still says it was not checked live`() {
        notifier.notify(alarm(), ReminderContent.Upcoming(null, AlarmVerification.UNVERIFIED))

        assertTrue(text(1).contains("Not checked against live times"))
    }

    @Test
    fun `an ambiguous Luas match is called out`() {
        notifier.notify(alarm(), ReminderContent.Upcoming(now.plusSeconds(120), AlarmVerification.LIVE_VERIFIED, true))

        assertTrue(text(1).contains("could be one of several trams"))
    }

    @Test
    fun `a lost service and a missed reminder have their own wording`() {
        notifier.notify(alarm(code = 1), ReminderContent.ServiceLost)
        notifier.notify(alarm(id = "b", code = 2), ReminderContent.Missed(now.minusSeconds(120)))

        assertTrue(text(1).contains("No longer listed"))
        assertTrue(text(2).contains("too late"))
        assertTrue(text(2).contains("21:58"))
    }

    @Test
    fun `showing the same alarm again replaces its notification`() {
        notifier.notify(alarm(), ReminderContent.Upcoming(now.plusSeconds(180), AlarmVerification.LIVE_VERIFIED))
        notifier.notify(alarm(), ReminderContent.Upcoming(now.plusSeconds(180), AlarmVerification.LIVE_VERIFIED))

        assertEquals(1, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun `different alarms keep separate notifications`() {
        notifier.notify(alarm("a", 1), ReminderContent.ServiceLost)
        notifier.notify(alarm("b", 2), ReminderContent.ServiceLost)

        assertEquals(2, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun `with notifications turned off nothing is shown and the caller is told`() {
        shadowOf(manager).setNotificationsEnabled(false)

        val shown = notifier.notify(alarm(), ReminderContent.ServiceLost)

        assertFalse(shown)
        assertFalse(notifier.areNotificationsEnabled())
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test
    fun `dismissing removes only that alarm's notification`() {
        notifier.notify(alarm("a", 1), ReminderContent.ServiceLost)
        notifier.notify(alarm("b", 2), ReminderContent.ServiceLost)

        notifier.dismiss(alarm("a", 1))

        assertNull(notification(1))
        assertNotNull(notification(2))
    }

    @Test
    fun `notification taps retain their own alarm identity and reuse the running activity`() {
        notifier.notify(alarm("a", 1), ReminderContent.ServiceLost)
        notifier.notify(alarm("b", 2), ReminderContent.ServiceLost)
        val first = notification(1)!!.contentIntent
        val second = notification(2)!!.contentIntent
        assertNotEquals(first, second)
        val firstIntent = shadowOf(first).savedIntent
        val secondIntent = shadowOf(second).savedIntent
        assertEquals("a", ReminderIntents.alarmId(firstIntent))
        assertEquals("b", ReminderIntents.alarmId(secondIntent))
        assertTrue(firstIntent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertFalse(firstIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
    }

    @Test
    fun `ordinary launches and malformed reminder URIs cannot select an alarm`() {
        assertNull(ReminderIntents.alarmId(Intent()))
        assertNull(ReminderIntents.alarmId(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("luasreminder://other/a"))))
        assertNull(
            ReminderIntents.alarmId(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("luasreminder://alarm/a/b")))
        )
    }

    @Test
    fun `live reminders have a ticking countdown and open without cancelling tracking`() {
        notifier.notify(alarm(), ReminderContent.Upcoming(now.plusSeconds(180), AlarmVerification.LIVE_VERIFIED))
        val notification = requireNotNull(notification(1))
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals(now.plusSeconds(180).toEpochMilli(), notification.`when`)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertFalse(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
        verify { liveNotifications.start("a") }
    }

    @Test
    fun `live changes replace the existing notification silently and offline disables the countdown`() {
        notifier.notify(alarm(), ReminderContent.Upcoming(now.plusSeconds(180), AlarmVerification.LIVE_VERIFIED))
        assertTrue(
            notifier.update(
                "a",
                ReminderContent.Upcoming(
                    now.plusSeconds(300),
                    AlarmVerification.LIVE_VERIFIED,
                    lastCheckedAt = now
                )
            )
        )
        assertEquals("Due in 5 minutes", text(1))
        assertEquals(1, shadowOf(manager).allNotifications.size)
        assertTrue(requireNotNull(notification(1)).flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
        notifier.update(
            "a",
            ReminderContent.Upcoming(
                now.plusSeconds(300),
                AlarmVerification.UNVERIFIED,
                lastCheckedAt = now
            )
        )
        assertFalse(requireNotNull(notification(1)).extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(text(1).contains("Live updates are unavailable"))
    }

    @Test
    fun `dismiss cancels tracking and a late network result cannot restore the notification`() {
        notifier.notify(alarm(), ReminderContent.Upcoming(now.plusSeconds(180), AlarmVerification.LIVE_VERIFIED))
        notifier.dismiss(alarm())
        assertFalse(
            notifier.update("a", ReminderContent.Upcoming(now.plusSeconds(300), AlarmVerification.LIVE_VERIFIED))
        )
        assertNull(notification(1))
        verify { liveNotifications.stop("a") }
    }

    @Test
    fun `Android refusing background tracking leaves an honest notification that dismisses on tap`() {
        every { liveNotifications.start(any()) } returns false
        notifier.notify(alarm(), ReminderContent.Upcoming(now.plusSeconds(180), AlarmVerification.LIVE_VERIFIED))
        val shown = requireNotNull(notification(1))
        assertFalse(shown.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(shown.flags and Notification.FLAG_AUTO_CANCEL != 0)
        assertFalse(shown.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(text(1).contains("Live updates are unavailable"))
    }

    @Test
    fun `tracking completion removes ongoing status and permits automatic cancellation on tap`() {
        notifier.notify(alarm(), ReminderContent.Upcoming(now.plusSeconds(180), AlarmVerification.LIVE_VERIFIED))
        notifier.finishTracking("a", ReminderContent.TrackingFinished(now.plusSeconds(180)))
        val shown = requireNotNull(notification(1))
        assertFalse(shown.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(shown.flags and Notification.FLAG_AUTO_CANCEL != 0)
        assertTrue(text(1).contains("tracking has finished"))
        assertNull(notifier.snapshot("a"))
    }

    @Test
    fun `a process restart clears live countdowns for orphaned notifications`() {
        notifier.notify(alarm(), ReminderContent.Upcoming(now.plusSeconds(180), AlarmVerification.LIVE_VERIFIED))
        val navigator = mockk<ServiceNavigator> { every { mainActivityClass() } returns MainActivity::class.java }
        val restarted =
            AlarmNotificationNotifier(context, Clock.fixed(now, ZoneOffset.UTC), navigator, liveNotifications)
        restarted.stopOrphanedTracking()
        val shown = requireNotNull(notification(1))
        assertFalse(shown.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertFalse(shown.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(shown.flags and Notification.FLAG_AUTO_CANCEL != 0)
        assertTrue(text(1).contains("Live updates are unavailable"))
        verify(exactly = 1) { liveNotifications.start("a") }
    }

    @Test
    fun `notifications from older app versions become dismissible snapshots`() {
        notifier.notify(alarm(), ReminderContent.Upcoming(now.plusSeconds(180), AlarmVerification.LIVE_VERIFIED))
        val old = requireNotNull(notification(1))
        old.extras.remove(EXTRA_TRACKING)
        manager.cancel(1)
        manager.notify("luas-alarm", 1, old)
        notifier.stopOrphanedTracking()
        val restored = requireNotNull(shadowOf(manager).getNotification("luas-alarm", 1))
        assertFalse(restored.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(restored.flags and Notification.FLAG_AUTO_CANCEL != 0)
        assertFalse(restored.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
    }
}
