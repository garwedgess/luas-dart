package com.wedgess.luas

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import com.wedgess.luas.data.alarm.LiveReminderService
import com.wedgess.luas.data.alarm.ReminderNotificationFactory
import com.wedgess.luas.domain.alarm.ReminderContent
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmRequest
import com.wedgess.luas.domain.model.AlarmVerification
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.navigation.service.ServiceNavigatorImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.util.UUID

/** Exercises the real foreground service, shared notification identity, and receiver dismissal routing. */
class LiveReminderServiceUiTest {
    @Test
    fun twoRemindersShareTheForegroundServiceAndDismissalTransfersThenStopsItsHost() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        val app = activity.application as LuasApplication
        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val connectivity = app.getSystemService(ConnectivityManager::class.java)
        val wifi = Settings.Global.getInt(app.contentResolver, "wifi_on", 0)
        val mobile = Settings.Global.getInt(app.contentResolver, "mobile_data", 0)
        val created = mutableListOf<Alarm>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        try {
            // Offline keeps this integration test independent of the public transit APIs.
            shell("svc data disable")
            shell("svc wifi disable")
            awaitCondition {
                connectivity.getNetworkCapabilities(connectivity.activeNetwork)
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) != true
            }
            repeat(2) { index ->
                val now = Instant.now()
                val alarm = runBlocking {
                    app.alarmCoordinator.create(
                        AlarmRequest(
                            requestId = "live-ui-${UUID.randomUUID()}",
                            kind = AlarmKind.SPECIFIC_DEPARTURE,
                            transport = TransportType.LUAS,
                            stationCode = "TEST",
                            stationName = "Live test station",
                            direction = DepartureDirection.INBOUND,
                            destination = "Test destination $index",
                            notifyMinutesBefore = 3,
                            expectedAt = now.plusSeconds(900),
                            observedAt = now
                        )
                    ).getOrThrow()
                }
                created += alarm
                assertTrue(
                    app.alarmNotifier.notify(
                        alarm,
                        ReminderContent.Upcoming(alarm.targetExpectedAt, AlarmVerification.UNVERIFIED)
                    )
                )
            }
            fun shown(alarm: Alarm) = manager.activeNotifications.firstOrNull {
                it.id == alarm.requestCode && it.tag == null
            }?.notification
            awaitCondition {
                shown(
                    created[0]
                )?.flags?.and(Notification.FLAG_FOREGROUND_SERVICE) == Notification.FLAG_FOREGROUND_SERVICE &&
                    shown(created[1]) != null
            }
            assertEquals(2, manager.activeNotifications.count { n -> created.any { it.requestCode == n.id } })
            assertTrue(requireNotNull(shown(created[0])).flags and Notification.FLAG_ONGOING_EVENT != 0)
            assertFalse(requireNotNull(shown(created[0])).extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
            requireNotNull(shown(created[0])).contentIntent.send()
            instrumentation.waitForIdleSync()
            assertTrue(shown(created[0]) != null)
            requireNotNull(shown(created[0])).actions.first { it.title.toString() == "Dismiss" }.actionIntent.send()
            awaitCondition {
                shown(created[0]) == null &&
                    shown(
                        created[1]
                    )?.flags?.and(Notification.FLAG_FOREGROUND_SERVICE) == Notification.FLAG_FOREGROUND_SERVICE
            }
            requireNotNull(shown(created[1])).actions.first { it.title.toString() == "Dismiss" }.actionIntent.send()
            awaitCondition { created.all { shown(it) == null } && !trackerRunning(app) }
            // Allow queued foreground posts, service teardown and the visibility watchdog to complete.
            val deadline = SystemClock.elapsedRealtime() + 6_000
            while (SystemClock.elapsedRealtime() < deadline) {
                assertTrue("A dismissed reminder reappeared", created.all { shown(it) == null })
                SystemClock.sleep(100)
            }
        } finally {
            try {
                runBlocking { created.forEach { app.alarmCoordinator.cancel(it.id) } }
            } finally {
                shell("svc wifi ${if (wifi == 1) "enable" else "disable"}")
                shell("svc data ${if (mobile == 1) "enable" else "disable"}")
                instrumentation.runOnMainSync { activity.finishAndRemoveTask() }
            }
        }
    }

    @Test
    @SuppressLint("MissingPermission") // Granted before posting the blocked-tracking fixture.
    fun backgroundBlockedReminderCanResumeAndItsDeleteIntentDismissesPermanently() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        val app = activity.application as LuasApplication
        val manager = app.getSystemService(NotificationManager::class.java)
        val connectivity = app.getSystemService(ConnectivityManager::class.java)
        val wifi = Settings.Global.getInt(app.contentResolver, "wifi_on", 0)
        val mobile = Settings.Global.getInt(app.contentResolver, "mobile_data", 0)
        var created: Alarm? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        try {
            shell("svc data disable")
            shell("svc wifi disable")
            awaitCondition {
                connectivity.getNetworkCapabilities(connectivity.activeNetwork)
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) != true
            }
            val now = Instant.now()
            val alarm = runBlocking {
                app.alarmCoordinator.create(
                    AlarmRequest(
                        requestId = "resume-ui-${UUID.randomUUID()}", kind = AlarmKind.SPECIFIC_DEPARTURE,
                        transport = TransportType.DART, stationCode = "TEST", stationName = "Resume test station",
                        direction = DepartureDirection.NORTHBOUND, destination = "Test destination",
                        notifyMinutesBefore = 3, expectedAt = now.plusSeconds(179), observedAt = now
                    )
                ).getOrThrow().also { created = it }
            }
            // Use the real delivery transition so Resume reads a durable FIRED reminder.
            runBlocking { app.alarmCoordinator.onAlarmDue(alarm.id) }
            fun shown() = manager.activeNotifications.firstOrNull { it.id == alarm.requestCode && it.tag == null }
                ?.notification
            awaitCondition {
                shown()?.flags?.and(Notification.FLAG_FOREGROUND_SERVICE) == Notification.FLAG_FOREGROUND_SERVICE
            }
            app.stopService(Intent(app, LiveReminderService::class.java))
            awaitCondition { !trackerRunning(app) }
            instrumentation.waitForIdleSync()
            // Android removes a service notification when it stops the service. Reproduce the fallback
            // posted when Android denies startup instead, then exercise its real Resume pending intent.
            val factory = ReminderNotificationFactory(app, Clock.systemUTC(), ServiceNavigatorImpl())
            val blocked = ReminderContent.Upcoming(
                alarm.targetExpectedAt,
                AlarmVerification.UNVERIFIED,
                backgroundRestricted = true
            )
            manager.notify(alarm.requestCode, factory.build(alarm, blocked, tracking = false))
            awaitCondition { shown()?.actions?.any { it.title.toString() == "Resume updates" } == true }
            requireNotNull(shown()).actions.first { it.title.toString() == "Resume updates" }.actionIntent.send()
            awaitCondition {
                shown()?.flags?.and(Notification.FLAG_FOREGROUND_SERVICE) == Notification.FLAG_FOREGROUND_SERVICE
            }
            requireNotNull(shown()).deleteIntent.send()
            awaitCondition { shown() == null && !trackerRunning(app) }
            val deadline = SystemClock.elapsedRealtime() + 6_000
            while (SystemClock.elapsedRealtime() < deadline) {
                assertTrue("A deleted reminder reappeared", shown() == null)
                SystemClock.sleep(100)
            }
        } finally {
            try {
                runBlocking { created?.let { app.alarmCoordinator.cancel(it.id) } }
            } finally {
                shell("svc wifi ${if (wifi == 1) "enable" else "disable"}")
                shell("svc data ${if (mobile == 1) "enable" else "disable"}")
                instrumentation.runOnMainSync { activity.finishAndRemoveTask() }
            }
        }
    }

    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 45_000
        while (!condition() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
        assertTrue("Notification lifecycle condition did not become true", condition())
    }

    @Suppress("DEPRECATION") // Android still exposes the caller's own services.
    private fun trackerRunning(context: Context): Boolean =
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == LiveReminderService::class.java.name }

    private fun shell(command: String) {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }
}
