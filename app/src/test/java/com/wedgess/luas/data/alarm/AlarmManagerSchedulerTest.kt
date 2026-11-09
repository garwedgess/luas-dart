package com.wedgess.luas.data.alarm

import android.app.AlarmManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmRegistration
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.TransportType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class AlarmManagerSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var scheduler: AlarmManagerScheduler
    private val now = Instant.parse("2026-11-09T22:00:00Z")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduler = AlarmManagerScheduler(context)
    }

    private fun alarm(id: String) = Alarm(
        id = id,
        requestCode = 1,
        kind = AlarmKind.SPECIFIC_DEPARTURE,
        state = AlarmState.ACTIVE,
        transport = TransportType.LUAS,
        stationCode = "STI",
        stationName = "Stillorgan",
        notifyMinutesBefore = 3,
        reminderAt = now,
        createdAt = now,
        updatedAt = now
    )

    private val scheduled get() = shadowOf(alarmManager).scheduledAlarms

    @Test
    fun `with exact alarm permission the alarm is registered exactly, waking the device, even in Doze`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        val registration = scheduler.register(alarm("a"), now.plusSeconds(600))

        assertEquals(AlarmRegistration.EXACT, registration)
        val registered = scheduled.single()
        assertEquals(AlarmManager.RTC_WAKEUP, registered.type)
        assertEquals(now.plusSeconds(600).toEpochMilli(), registered.triggerAtMs)
        assertTrue(registered.allowWhileIdle)
    }

    @Test
    fun `without exact alarm permission it falls back to an inexact registration that still works in Doze`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        val registration = scheduler.register(alarm("a"), now.plusSeconds(600))

        assertEquals(AlarmRegistration.INEXACT, registration)
        assertEquals(1, scheduled.size)
        assertTrue(scheduled.single().allowWhileIdle)
        assertEquals(false, scheduler.canScheduleExactAlarms())
    }

    @Test
    fun `registering the same alarm again replaces its registration instead of adding another`() {
        scheduler.register(alarm("a"), now.plusSeconds(600))
        scheduler.register(alarm("a"), now.plusSeconds(900))

        assertEquals(1, scheduled.size)
        assertEquals(now.plusSeconds(900).toEpochMilli(), scheduled.single().triggerAtMs)
    }

    @Test
    fun `each alarm has its own registration and cancelling one leaves the others`() {
        scheduler.register(alarm("a"), now.plusSeconds(600))
        scheduler.register(alarm("b"), now.plusSeconds(700))
        scheduler.register(alarm("c"), now.plusSeconds(800))
        assertEquals(3, scheduled.size)

        scheduler.cancel("b")

        assertEquals(
            listOf(now.plusSeconds(600), now.plusSeconds(800)).map { it.toEpochMilli() },
            scheduled.map { it.triggerAtMs }.sorted()
        )
    }

    @Test
    fun `cancelling an alarm that was never registered is harmless`() {
        scheduler.cancel("never-registered")

        assertTrue(scheduled.isEmpty())
    }

    @Test
    fun `alarms are identified by their own id in the broadcast, not by shared fields`() {
        assertNotEquals(AlarmReceiver.alarmUri("a"), AlarmReceiver.alarmUri("b"))
        assertEquals("a", AlarmReceiver.alarmUri("a").lastPathSegment)
    }

    @Test
    fun `a permission revoked between the check and the call degrades to inexact instead of failing`() {
        val mockManager = mockk<AlarmManager>(relaxed = true)
        every { mockManager.canScheduleExactAlarms() } returns true
        every { mockManager.setExactAndAllowWhileIdle(any(), any(), any()) } throws SecurityException("revoked")
        val wrapped = object : ContextWrapper(context) {
            override fun getSystemService(name: String): Any? =
                if (name == Context.ALARM_SERVICE) mockManager else super.getSystemService(name)
        }

        val registration = AlarmManagerScheduler(wrapped).register(alarm("a"), now.plusSeconds(600))

        assertEquals(AlarmRegistration.INEXACT, registration)
        verify { mockManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, now.plusSeconds(600).toEpochMilli(), any()) }
    }

    @Test
    fun `settings are opened for the package when the user asks`() {
        scheduler.openExactAlarmSettings()

        val started: Intent = shadowOf(context as android.app.Application).nextStartedActivity
        assertEquals("package:${context.packageName}", started.data.toString())
    }
}
