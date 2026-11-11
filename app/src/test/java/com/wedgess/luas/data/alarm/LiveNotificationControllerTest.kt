package com.wedgess.luas.data.alarm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LiveNotificationControllerTest {
    private val context = mockk<Context>(relaxed = true)
    private val controller = AndroidLiveNotificationController(context)

    @Test
    fun `starting tracking targets the foreground service with its own alarm identity`() {
        val intent = slot<Intent>()
        every { context.startForegroundService(capture(intent)) } returns ComponentName("app", "service")
        assertTrue(controller.start("reminder-a"))
        assertEquals(LiveReminderService::class.java.name, intent.captured.component?.className)
        assertEquals("reminder-a", intent.captured.getStringExtra(LiveReminderService.EXTRA_ALARM_ID))
    }

    @Test
    fun `background start restrictions and permission denial are reported without a crash`() {
        every { context.startForegroundService(any()) } throws IllegalStateException("background")
        assertFalse(controller.start("a"))
        every { context.startForegroundService(any()) } throws SecurityException("permission")
        assertFalse(controller.start("a"))
    }

    @Test
    fun `dismissing an old notification only broadcasts to a running tracker and never starts a service`() {
        every { context.packageName } returns "com.wedgess.luas"
        val intent = slot<Intent>()
        every { context.sendBroadcast(capture(intent)) } returns Unit
        controller.stop("a")
        assertEquals(LiveReminderService.ACTION_DISMISS, intent.captured.action)
        assertEquals("com.wedgess.luas", intent.captured.`package`)
        assertEquals("a", intent.captured.getStringExtra(LiveReminderService.EXTRA_ALARM_ID))
        verify(exactly = 0) { context.startService(any()) }
        verify(exactly = 0) { context.startForegroundService(any()) }
    }
}
