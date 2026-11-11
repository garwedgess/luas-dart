package com.wedgess.luas.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import java.io.IOException

/** True when the network caused the failure (no connection, timeout, DNS), not bad data or a server error. */
fun Throwable.isOffline(): Boolean = generateSequence(this) { it.cause }.any { it is IOException }

/** Retry a failed or empty station catalogue when connectivity returns, preserving identical catalogue results. */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.restartOnReconnect(online: Flow<Boolean>): Flow<T> = kotlinx.coroutines.flow.flow {
    var first = true
    online.distinctUntilChanged().collect { connected ->
        if (first || connected) emit(Unit)
        first = false
    }
}.flatMapLatest { this }
