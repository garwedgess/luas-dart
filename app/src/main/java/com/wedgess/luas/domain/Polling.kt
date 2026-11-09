package com.wedgess.luas.domain

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

val DefaultBackoffMillis = listOf(5_000L, 10_000L, 20_000L, 40_000L, 60_000L)
private const val JITTER_DIVISOR = 5

/**
 * Fetches immediately, then every [intervalMillis]. A failed fetch is retried after the next step of [backoffMillis]
 * (plus up to 20% jitter) and the sequence resets after a success. Anything emitted on [refresh] triggers an
 * immediate fetch. Collection stops when the collector is cancelled, so scoping the collector to the lifecycle
 * scopes the polling.
 */
fun <T> pollFlow(
    intervalMillis: Long,
    refresh: Flow<Unit>,
    backoffMillis: List<Long> = DefaultBackoffMillis,
    random: Random = Random.Default,
    fetch: suspend () -> Result<T>
): Flow<Polled<T>> = channelFlow {
    val manualRefresh = Channel<Unit>(Channel.CONFLATED)
    launch { refresh.collect { manualRefresh.trySend(Unit) } }

    var last: T? = null
    var failures = 0
    while (true) {
        fetch().fold(
            onSuccess = {
                last = it
                failures = 0
                send(Polled(it))
            },
            onFailure = {
                failures++
                send(Polled(last, it))
            }
        )
        val wait = if (failures == 0) {
            intervalMillis
        } else {
            val base = backoffMillis[minOf(failures, backoffMillis.size) - 1]
            base + random.nextLong(base / JITTER_DIVISOR + 1)
        }
        withTimeoutOrNull(wait) { manualRefresh.receive() }
    }
}
