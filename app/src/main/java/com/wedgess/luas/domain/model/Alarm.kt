package com.wedgess.luas.domain.model

import java.time.Instant
import java.time.LocalDate

enum class AlarmKind {
    /** Follows the next qualifying service at a station; it may move to a later service as departures change. */
    NEXT_DEPARTURE,

    /** Pinned to one service and never silently switches to another. */
    SPECIFIC_DEPARTURE
}

enum class AlarmState {
    /** Waiting: an OS registration should exist for it. */
    ACTIVE,

    /** The reminder was delivered (or could not be shown, see [AlarmFinishReason]). */
    FIRED,
    CANCELLED,

    /** Ended without a reminder, e.g. the registration failed. */
    EXPIRED
}

enum class AlarmFinishReason {
    USER_CANCELLED,
    NOTIFIED,

    /** The pinned service was missing from a successful live result when the reminder was due. */
    SERVICE_LOST,

    /** The service was already due or past by the time the reminder could be delivered. */
    MISSED,
    NOTIFICATIONS_DISABLED,
    REGISTRATION_FAILED
}

/** Whether the delivered reminder was confirmed against a live result at the time it fired. */
enum class AlarmVerification { LIVE_VERIFIED, UNVERIFIED }

/** How the alarm is currently registered with the OS. An inexact registration may be delivered late. */
enum class AlarmRegistration { EXACT, INEXACT }

/**
 * Durable alarm definition. This is the source of truth: OS scheduling (AlarmManager) is rebuilt from the
 * [AlarmState.ACTIVE] alarms, and an alarm belongs to its own station regardless of which station is being viewed.
 *
 * [id] identifies the alarm request and never changes. It is separate from the departure the alarm follows, which is
 * described by the station, [direction], [destination] and, for DART, [tripCode] with [operatingDate].
 */
data class Alarm(
    val id: String,
    /** Small stable integer for the OS (notification id), assigned when the alarm is stored. */
    val requestCode: Int,
    val kind: AlarmKind,
    val state: AlarmState,
    val transport: TransportType,
    val stationCode: String,
    val stationName: String,
    /** Direction filter for [AlarmKind.NEXT_DEPARTURE], or the direction of the pinned service. */
    val direction: DepartureDirection? = null,
    /** Destination filter for [AlarmKind.NEXT_DEPARTURE], or the destination of the pinned service. */
    val destination: String? = null,
    val notifyMinutesBefore: Int,
    /** DART train code of the pinned service; null for Luas, which exposes no vehicle identity. */
    val tripCode: String? = null,
    val operatingDate: LocalDate? = null,
    /** Last live-observed expected arrival of the target and when it was observed. */
    val targetExpectedAt: Instant? = null,
    val targetObservedAt: Instant? = null,
    /** Wall-clock time the reminder is due. */
    val reminderAt: Instant,
    /** Live re-checks already done; bounded by [MAX_LIVE_CHECKS]. */
    val checkCount: Int = 0,
    val registration: AlarmRegistration? = null,
    val finishReason: AlarmFinishReason? = null,
    val verification: AlarmVerification? = null,
    val notifiedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        const val MAX_LIVE_CHECKS = 3
    }
}

/** What the user asked for. [requestId] makes creating the same request twice a no-op. */
data class AlarmRequest(
    val requestId: String,
    val kind: AlarmKind,
    val transport: TransportType,
    val stationCode: String,
    val stationName: String,
    val direction: DepartureDirection?,
    val destination: String?,
    val notifyMinutesBefore: Int,
    val tripCode: String? = null,
    val operatingDate: LocalDate? = null,
    /** Expected arrival of the chosen departure and when it was observed live. */
    val expectedAt: Instant,
    val observedAt: Instant
)

/** What the device currently allows. The two permissions are independent and both only degrade behaviour. */
data class AlarmCapabilities(val exactAlarms: Boolean, val notifications: Boolean)
