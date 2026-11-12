package com.wedgess.luas.data.alarm

import android.content.Intent
import com.wedgess.luas.domain.alarm.AlarmCoordinator
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The receiver only routes broadcasts; all behaviour is the coordinator's. */
@RunWith(RobolectricTestRunner::class)
class AlarmWorkRoutingTest {

    private val coordinator = mockk<AlarmCoordinator>(relaxed = true)

    private fun due(id: String) = Intent(AlarmReceiver.ACTION_ALARM_DUE).setData(AlarmReceiver.alarmUri(id))

    @Test
    fun `an alarm broadcast evaluates that alarm`() = runTest {
        alarmWork(due("alarm-1"), coordinator)!!.invoke()

        coVerify(exactly = 1) { coordinator.onAlarmDue("alarm-1") }
    }

    @Test
    fun `dismissing the notification only removes it`() = runTest {
        val intent = Intent(AlarmReceiver.ACTION_DISMISS).setData(AlarmReceiver.alarmUri("alarm-1"))

        alarmWork(intent, coordinator)!!.invoke()

        coVerify(exactly = 1) { coordinator.dismissNotification("alarm-1") }
        coVerify(exactly = 0) { coordinator.cancel(any()) }
    }

    @Test
    fun `system events that lose OS registrations rebuild them from the database`() = runTest {
        listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        ).forEach { alarmWork(Intent(it), coordinator)!!.invoke() }

        coVerify(exactly = 4) { coordinator.reconcile() }
    }

    @Test
    fun `an alarm broadcast without an alarm id is ignored`() {
        assertNull(alarmWork(Intent(AlarmReceiver.ACTION_ALARM_DUE), coordinator))
        assertNull(alarmWork(Intent(AlarmReceiver.ACTION_DISMISS), coordinator))
    }

    @Test
    fun `an id on a foreign scheme is ignored`() {
        val intent = Intent(
            AlarmReceiver.ACTION_ALARM_DUE
        ).setData(android.net.Uri.parse("https://example.com/alarm/x"))

        assertNull(alarmWork(intent, coordinator))
    }

    @Test
    fun `unrelated broadcasts are ignored`() {
        assertNull(alarmWork(Intent("android.intent.action.SOMETHING_ELSE"), coordinator))
        assertNull(alarmWork(Intent(), coordinator))
    }

    @Test
    fun `resume routes the saved ETA to tracking without firing the reminder again`() = runTest {
        val expected = java.time.Instant.parse("2026-11-11T12:00:00Z")
        val intent = Intent(AlarmReceiver.ACTION_RESUME).setData(AlarmReceiver.alarmUri("alarm-1"))
            .putExtra(AlarmReceiver.EXTRA_EXPECTED_AT, expected.toEpochMilli())
        alarmWork(intent, coordinator)!!.invoke()
        coVerify(exactly = 1) { coordinator.resumeNotification("alarm-1", expected) }
        coVerify(exactly = 0) { coordinator.onAlarmDue(any()) }
    }
}
