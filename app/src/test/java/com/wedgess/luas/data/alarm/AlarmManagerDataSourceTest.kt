package com.wedgess.luas.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.wedgess.luas.data.model.NotificationData
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class AlarmManagerDataSourceTest {

    @RelaxedMockK
    private lateinit var context: Context
    @RelaxedMockK
    private lateinit var alarmManager: AlarmManager
    @RelaxedMockK
    private lateinit var powerManager: PowerManager
    @RelaxedMockK
    private lateinit var pendingIntent: PendingIntent

    private lateinit var alarmManagerDataSource: AlarmManagerDataSource

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { context.packageName } returns "com.wedgess.luas"

        mockkStatic(PendingIntent::class)
        every {
            PendingIntent.getBroadcast(
                any(),
                any(),
                any(),
                any()
            )
        } returns pendingIntent

        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()

        alarmManagerDataSource = AlarmManagerDataSource(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `scheduleAlarm sets exact alarm on Android S and above`() {
        // Given
        val triggerTimeMillis = 1000L
        val notificationData = mockk<NotificationData>()
        val intentSlot = slot<Intent>()

        // When
        alarmManagerDataSource.scheduleAlarm(triggerTimeMillis, notificationData)

        // Then
        verify {
            PendingIntent.getBroadcast(
                context,
                AlarmManagerDataSource.ALARM_REQUEST_CODE,
                capture(intentSlot),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }

        assertEquals(AlarmReceiver.EXTRA_ALARM_ID, intentSlot.captured.extras?.keySet()?.find { it == AlarmReceiver.EXTRA_ALARM_ID })
        assertEquals(AlarmReceiver.EXTRA_NOTIFICATION_DATA, intentSlot.captured.extras?.keySet()?.find { it == AlarmReceiver.EXTRA_NOTIFICATION_DATA })
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
    fun `scheduleAlarm sets exact alarm on Android below M`() {
        // Given
        val triggerTimeMillis = 1000L
        val notificationData = mockk<NotificationData>()

        // When
        alarmManagerDataSource.scheduleAlarm(triggerTimeMillis, notificationData)

        // Then
        verify {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTimeMillis,
                any()
            )
        }
    }

    @Test
    fun `cancelAlarm cancels alarm and pending intent`() {
        // Given
        every { pendingIntent.cancel() } just Runs
        every { alarmManager.cancel(any<PendingIntent>()) } just Runs

        // When
        alarmManagerDataSource.cancelAlarm()

        // Then
        verify { alarmManager.cancel(pendingIntent) }
        verify { pendingIntent.cancel() }
    }

    @Test
    fun `isAlarmSet returns true when pending intent exists`() {
        // Given
        every {
            PendingIntent.getBroadcast(
                any(),
                any(),
                any(),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        } returns pendingIntent

        // When
        val result = runBlocking { alarmManagerDataSource.alarmStateFlow.first() }

        // Then
        assertTrue(result)
    }

    @Test
    fun `isAlarmSet returns false when pending intent does not exist`() {
        // Given
        every {
            PendingIntent.getBroadcast(
                any(),
                any(),
                any(),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        } returns null

        // Need to recreate the data source to initialize with mock value
        alarmManagerDataSource = AlarmManagerDataSource(context)

        // When
        val result = runBlocking { alarmManagerDataSource.alarmStateFlow.first() }

        // Then
        assertFalse(result)
    }

    @Test
    fun `canScheduleExactAlarms returns alarmManager result on Android S and above`() {
        // Given
        every { alarmManager.canScheduleExactAlarms() } returns true

        // When
        val result = alarmManagerDataSource.canScheduleExactAlarms()

        // Then
        assertTrue(result)
        verify { alarmManager.canScheduleExactAlarms() }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `canScheduleExactAlarms returns true on Android below S`() {
        // When
        val result = alarmManagerDataSource.canScheduleExactAlarms()

        // Then
        assertTrue(result)
    }

    @Test
    fun `openExactAlarmPermissionSettings starts activity with correct intent`() {
        // Given
        val intentSlot = slot<Intent>()
        every { context.startActivity(capture(intentSlot)) } just Runs

        // When
        alarmManagerDataSource.openExactAlarmPermissionSettings()

        // Then
        verify { context.startActivity(any()) }
        assertEquals(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, intentSlot.captured.action)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intentSlot.captured.flags)
    }

    @Test
    fun `isBatteryOptimizationIgnored returns power manager result`() {
        // Given
        every { powerManager.isIgnoringBatteryOptimizations(any()) } returns true

        // When
        val result = alarmManagerDataSource.isBatteryOptimizationIgnored()

        // Then
        assertTrue(result)
        verify { powerManager.isIgnoringBatteryOptimizations("com.wedgess.luas") }
    }

    @Test
    fun `openBatteryOptimizationSettings starts activity with correct intent`() {
        // Given
        val intentSlot = slot<Intent>()
        every { context.startActivity(capture(intentSlot)) } just Runs

        // When
        alarmManagerDataSource.openBatteryOptimizationSettings()

        // Then
        verify { context.startActivity(any()) }
        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, intentSlot.captured.action)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intentSlot.captured.flags)
    }
}
