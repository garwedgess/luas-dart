package com.wedgess.luas.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.random.Random

val DefaultBackoffMillis = listOf(5_000L, 10_000L, 20_000L, 40_000L, 60_000L)
private const val JITTER_DIVISOR = 5

/**
 * Fetches immediately, then every [intervalMillis]. A failed fetch is retried after the next step of [backoffMillis]
 * (plus up to 20% jitter) and the sequence resets after a success. Anything emitted on [refresh] triggers an
 * immediate fetch. Collection stops when the collector is cancelled, so scoping the collector to the lifecycle
 * scopes the polling. [initial] is stored data to emit (as not live) before the first fetch.
 * Losing [online] cancels an in-flight request and pauses polling. Reconnecting fetches immediately.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> pollFlow(
    intervalMillis: Long,
    refresh: Flow<Unit>,
    initial: T? = null,
    backoffMillis: List<Long> = DefaultBackoffMillis,
    random: Random = Random.Default,
    online: Flow<Boolean> = flowOf(true),
    fetch: suspend () -> Result<T>
): Flow<Polled<T>> = channelFlow {
    var last = initial
    var wasOffline = false
    val schedule = PollSchedule(intervalMillis, backoffMillis, random)
    online.distinctUntilChanged().collectLatest { connected ->
        if (connected) {
            if (wasOffline && last == null) send(Polled(null, isCached = true))
            wasOffline = false
            pollWhileOnline(schedule, refresh, last, fetch).collect {
                last = it.data
                send(it)
            }
        } else {
            wasOffline = true
            val offline = IOException("No internet connection")
            while (true) {
                send(Polled(last, offline))
                // Only age the cached rows; there are no network requests or queued refreshes while offline.
                if (last == null) awaitCancellation()
                delay(intervalMillis)
            }
        }
    }
}

private data class PollSchedule(val intervalMillis: Long, val backoffMillis: List<Long>, val random: Random)

private fun <T> pollWhileOnline(
    schedule: PollSchedule,
    refresh: Flow<Unit>,
    initial: T?,
    fetch: suspend () -> Result<T>
): Flow<Polled<T>> = channelFlow {
    val manualRefresh = Channel<Unit>(Channel.CONFLATED)
    launch { refresh.collect { manualRefresh.trySend(Unit) } }

    var last: T? = initial
    var failures = 0
    // Stored data is shown straight away, but flagged as not live until the first fetch succeeds.
    initial?.let { send(Polled(it, isCached = true)) }
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
            schedule.intervalMillis
        } else {
            val backoff = schedule.backoffMillis
            val base = backoff[minOf(failures, backoff.size) - 1]
            base + schedule.random.nextLong(base / JITTER_DIVISOR + 1)
        }
        withTimeoutOrNull(wait) { manualRefresh.receive() }
    }
}
