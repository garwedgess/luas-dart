package com.wedgess.luas.domain

import java.io.IOException

/** True when the network caused the failure (no connection, timeout, DNS), not bad data or a server error. */
fun Throwable.isOffline(): Boolean = generateSequence(this) { it.cause }.any { it is IOException }
