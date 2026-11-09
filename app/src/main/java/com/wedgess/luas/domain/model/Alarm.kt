package com.wedgess.luas.domain.model

import java.time.Instant
import java.time.LocalDate

enum class AlarmKind {
    /** Follows the next qualifying service at a station; it may move to a later service as departures change. */
    NEXT_DEPARTURE,

    /** Pinned to one service and never silently switches to another. */
    SPECIFIC_DEPARTURE
}

enum class AlarmState { ACTIVE, FIRED, CANCELLED }

/**
 * Durable alarm definition. This is the source of truth: OS scheduling (AlarmManager) is rebuilt from the
 * [AlarmState.ACTIVE] alarms, and an alarm belongs to its own station regardless of which station is being viewed.
 */
data class Alarm(
    val id: String,
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
    /** Last expected arrival of the target and when it was observed live, so a reminder can be rebuilt. */
    val targetExpectedAt: Instant? = null,
    val targetObservedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        /** One next-departure alarm per station and direction, so saving it again updates the same record. */
        fun nextDepartureId(transport: TransportType, stationCode: String, direction: DepartureDirection?) =
            "next:${transport.name}:$stationCode:${direction?.name ?: "ANY"}"

        /** A DART service is identified by its train code and operating date at a station. */
        fun departureId(transport: TransportType, stationCode: String, tripCode: String, operatingDate: LocalDate) =
            "dep:${transport.name}:$stationCode:$tripCode:$operatingDate"
    }
}
