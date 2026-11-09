package com.wedgess.luas.data.mapper

import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmFinishReason
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmRegistration
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.AlarmVerification
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.TransportType
import java.time.Instant
import java.time.LocalDate
import com.wedgess.luas.Alarm as AlarmRow

fun Alarm.toRow() = AlarmRow(
    Id = id,
    RequestCode = requestCode.toLong(),
    Kind = kind.name,
    State = state.name,
    Transport = transport.name,
    StationCode = stationCode,
    StationName = stationName,
    Direction = direction?.name,
    Destination = destination,
    NotifyMinutesBefore = notifyMinutesBefore.toLong(),
    TripCode = tripCode,
    OperatingDate = operatingDate?.toString(),
    TargetExpectedAtMillis = targetExpectedAt?.toEpochMilli(),
    TargetObservedAtMillis = targetObservedAt?.toEpochMilli(),
    ReminderAtMillis = reminderAt.toEpochMilli(),
    CheckCount = checkCount.toLong(),
    Registration = registration?.name,
    FinishReason = finishReason?.name,
    Verification = verification?.name,
    NotifiedAtMillis = notifiedAt?.toEpochMilli(),
    CreatedAtMillis = createdAt.toEpochMilli(),
    UpdatedAtMillis = updatedAt.toEpochMilli()
)

fun AlarmRow.toAlarm() = Alarm(
    id = Id,
    requestCode = RequestCode.toInt(),
    kind = AlarmKind.valueOf(Kind),
    state = AlarmState.valueOf(State),
    transport = TransportType.valueOf(Transport),
    stationCode = StationCode,
    stationName = StationName,
    direction = Direction?.let { name -> DepartureDirection.entries.firstOrNull { it.name == name } },
    destination = Destination,
    notifyMinutesBefore = NotifyMinutesBefore.toInt(),
    tripCode = TripCode,
    operatingDate = OperatingDate?.let(LocalDate::parse),
    targetExpectedAt = TargetExpectedAtMillis?.let(Instant::ofEpochMilli),
    targetObservedAt = TargetObservedAtMillis?.let(Instant::ofEpochMilli),
    reminderAt = Instant.ofEpochMilli(ReminderAtMillis),
    checkCount = CheckCount.toInt(),
    registration = Registration?.let { name -> AlarmRegistration.entries.firstOrNull { it.name == name } },
    finishReason = FinishReason?.let { name -> AlarmFinishReason.entries.firstOrNull { it.name == name } },
    verification = Verification?.let { name -> AlarmVerification.entries.firstOrNull { it.name == name } },
    notifiedAt = NotifiedAtMillis?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(CreatedAtMillis),
    updatedAt = Instant.ofEpochMilli(UpdatedAtMillis)
)
