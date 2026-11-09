package com.wedgess.luas.domain.alarm

import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmRegistration
import com.wedgess.luas.domain.model.AlarmVerification
import java.time.Instant

/** OS alarm registration. At most one registration exists per alarm, replaced on every call. */
interface AlarmScheduler {

    fun canScheduleExactAlarms(): Boolean

    /**
     * Registers the alarm to wake the app at [triggerAt], exactly if permitted and otherwise with an inexact
     * (possibly delayed) registration, and reports which was used.
     */
    fun register(alarm: Alarm, triggerAt: Instant): AlarmRegistration

    fun cancel(alarmId: String)

    fun openExactAlarmSettings()
}

sealed interface ReminderContent {
    /** The service is expected around [expectedAt]. [ambiguous] means it could be one of several Luas trams. */
    data class Upcoming(
        val expectedAt: Instant?,
        val verification: AlarmVerification,
        val ambiguous: Boolean = false
    ) : ReminderContent

    /** The pinned service was missing from live results; nothing is claimed about why. */
    data object ServiceLost : ReminderContent

    /** The reminder came too late: the service was already due at [expectedAt]. */
    data class Missed(val expectedAt: Instant?) : ReminderContent
}

interface AlarmNotifier {

    fun areNotificationsEnabled(): Boolean

    /** Shows the reminder for the alarm, replacing any earlier one for it. Returns false if it could not be shown. */
    fun notify(alarm: Alarm, content: ReminderContent): Boolean

    fun dismiss(alarm: Alarm)
}
