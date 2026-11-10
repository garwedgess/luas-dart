package com.wedgess.luas.data.utils.extensions

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest

private const val INITIAL_RETRY_MILLIS = 5_000L
private const val MAX_RETRY_MILLIS = 60_000L

/** Retry only an empty catalogue, and only while it is observed by a foreground screen. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Flow<List<T>>.populateWhenEmpty(refresh: suspend () -> Unit): Flow<List<T>> = transformLatest { rows ->
    if (rows.isNotEmpty()) {
        emit(rows)
    } else {
        var retryMillis = INITIAL_RETRY_MILLIS
        while (true) {
            refresh()
            emit(rows)
            delay(retryMillis)
            retryMillis = (retryMillis * 2).coerceAtMost(MAX_RETRY_MILLIS)
        }
    }
}
