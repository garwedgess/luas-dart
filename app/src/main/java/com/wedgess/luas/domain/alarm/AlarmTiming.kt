package com.wedgess.luas.domain.alarm

import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmFinishReason
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.AlarmVerification
import java.time.Duration
import java.time.Instant

/** The reminder fires this long before the requested lead so an inexact registration still lands in time. */
internal val ReminderSafety: Duration = Duration.ofSeconds(30)

/** The first (and only early) live re-check happens this long before the reminder. */
internal val CheckLead: Duration = Duration.ofMinutes(5)
private val CheckMinGap: Duration = Duration.ofMinutes(1)
private val MinDelay: Duration = Duration.ofSeconds(2)

/** A wake-up this close to (or after) the reminder time is the delivery, not another re-check. */
internal val FireSlack: Duration = Duration.ofSeconds(45)

/** Live result wait, short enough for a broadcast receiver to finish within the OS limit. */
internal val LiveTimeout: Duration = Duration.ofSeconds(6)

/** A service this long past its expected time has already come and gone. */
internal val MissedGrace: Duration = Duration.ofSeconds(60)

/** Missing from live results this long before its expected time means it is not just about to arrive. */
private val ServiceLostThreshold: Duration = Duration.ofSeconds(90)
internal val FinishedRetention: Duration = Duration.ofDays(7)

internal fun reminderTime(expectedAt: Instant, notifyMinutesBefore: Int): Instant =
    expectedAt.minus(Duration.ofMinutes(notifyMinutesBefore.toLong())).minus(ReminderSafety)

/**
 * The single next OS wake-up for an alarm: one early live re-check while re-checks remain and there is room for it,
 * otherwise the delivery itself. Never earlier than shortly after [now].
 */
internal fun nextTrigger(alarm: Alarm, now: Instant): Instant {
    val checkAt = alarm.reminderAt.minus(CheckLead)
    val useCheck = alarm.checkCount < Alarm.MAX_LIVE_CHECKS && checkAt.isAfter(now.plus(CheckMinGap))
    val trigger = if (useCheck) checkAt else alarm.reminderAt
    val earliest = now.plus(MinDelay)
    return if (trigger.isBefore(earliest)) earliest else trigger
}

internal sealed interface DeliveryDecision {
    /** Live data says the service is later than planned and re-checks remain, so wait instead of firing early. */
    data class Defer(val found: AlarmMatch.Found) : DeliveryDecision

    data class Notify(
        val state: AlarmState,
        val reason: AlarmFinishReason,
        val verification: AlarmVerification,
        val content: ReminderContent
    ) : DeliveryDecision
}

/**
 * What to do when the reminder is due, from what is known right now. Without a live match the reminder is still
 * delivered but never represented as live-verified; a pinned service that a successful live result does not list is
 * reported as lost rather than silently dropped or replaced; a reminder that arrives after the service was due is
 * reported as missed rather than as upcoming.
 */
internal fun decideDelivery(alarm: Alarm, hadLive: Boolean, match: AlarmMatch?, now: Instant): DeliveryDecision {
    val found = match as? AlarmMatch.Found
    val expected = found?.departure?.expectedAt ?: alarm.targetExpectedAt
    if (found != null && isDelayedBeyondReminder(alarm, found, now)) return DeliveryDecision.Defer(found)

    val lost = hadLive && match is AlarmMatch.NotFound && expected != null &&
        Duration.between(now, expected) > ServiceLostThreshold
    val missed = expected != null && expected.plus(MissedGrace).isBefore(now)
    val verification = if (hadLive) AlarmVerification.LIVE_VERIFIED else AlarmVerification.UNVERIFIED
    return when {
        lost -> DeliveryDecision.Notify(
            AlarmState.FIRED,
            AlarmFinishReason.SERVICE_LOST,
            verification,
            ReminderContent.ServiceLost
        )

        missed -> DeliveryDecision.Notify(
            AlarmState.EXPIRED,
            AlarmFinishReason.MISSED,
            verification,
            ReminderContent.Missed(expected)
        )

        else -> {
            val confirmed = if (found != null) AlarmVerification.LIVE_VERIFIED else AlarmVerification.UNVERIFIED
            DeliveryDecision.Notify(
                AlarmState.FIRED,
                AlarmFinishReason.NOTIFIED,
                confirmed,
                ReminderContent.Upcoming(expected, confirmed, ambiguous = found?.ambiguous == true)
            )
        }
    }
}

private fun isDelayedBeyondReminder(alarm: Alarm, found: AlarmMatch.Found, now: Instant): Boolean {
    val expected = found.departure.expectedAt ?: return false
    return reminderTime(expected, alarm.notifyMinutesBefore).isAfter(now.plus(FireSlack)) &&
        alarm.checkCount < Alarm.MAX_LIVE_CHECKS
}
