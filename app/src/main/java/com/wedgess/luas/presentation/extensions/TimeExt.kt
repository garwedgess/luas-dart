package com.wedgess.luas.presentation.extensions

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val IRELAND_ZONE: ZoneId = ZoneId.of("Europe/Dublin")
private val CLOCK_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Formats the instant as local Irish wall-clock time (e.g. "16:35"), which is how both transport APIs report times. */
fun Instant.toClockTime(zone: ZoneId = IRELAND_ZONE): String = CLOCK_TIME_FORMAT.format(atZone(zone))
