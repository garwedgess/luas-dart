package com.wedgess.luas.domain

import app.cash.turbine.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class PollingTest {

    private val interval = 15_000L
    private val backoff = listOf(1_000L, 2_000L, 4_000L)

    private fun <T> poll(refresh: kotlinx.coroutines.flow.Flow<Unit> = emptyFlow(), fetch: suspend () -> Result<T>) =
        pollFlow(interval, refresh, backoffMillis = backoff, random = Random(0), fetch = fetch)

    @Test
    fun `fetches immediately then once per interval`() = runTest {
        var calls = 0
        poll { Result.success(++calls) }.test {
            assertEquals(1, awaitItem().data)
            expectNoEvents()
            advanceTimeBy(interval - 1)
            expectNoEvents()
            advanceTimeBy(1)
            assertEquals(2, awaitItem().data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keeps last data and reports the error when a fetch fails`() = runTest {
        val results = ArrayDeque(listOf(Result.success("live"), Result.failure<String>(IllegalStateException("down"))))
        poll { results.removeFirst() }.test {
            assertEquals(Polled("live"), awaitItem())
            advanceTimeBy(interval)
            val failed = awaitItem()
            assertEquals("live", failed.data)
            assertEquals("down", failed.error?.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reports an error without data when the first fetch fails`() = runTest {
        poll<String> { Result.failure(IllegalStateException("down")) }.test {
            val first = awaitItem()
            assertNull(first.data)
            assertEquals("down", first.error?.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Virtual-time gaps (ms) between consecutive fetches when each fetch returns the next scripted result. */
    private fun TestScope.fetchGaps(results: List<Result<Int>>, runForMillis: Long): List<Long> {
        val queue = ArrayDeque(results)
        val times = mutableListOf<Long>()
        val job = launch {
            poll {
                times += testScheduler.currentTime
                queue.removeFirstOrNull() ?: Result.failure(Exception("end"))
            }
                .collect { }
        }
        testScheduler.advanceTimeBy(runForMillis)
        testScheduler.runCurrent()
        job.cancel()
        return times.zipWithNext { a, b -> b - a }
    }

    @Test
    fun `retries with increasing backoff capped at the last step`() = runTest {
        val gaps = fetchGaps(results = emptyList(), runForMillis = 30_000)

        // Each wait is the backoff step plus at most 20% jitter, never the 15s polling interval.
        assertTrue(gaps[0] in 1_000L..1_200L)
        assertTrue(gaps[1] in 2_000L..2_400L)
        assertTrue(gaps[2] in 4_000L..4_800L)
        assertTrue("capped at the last step", gaps.drop(2).all { it in 4_000L..4_800L })
    }

    @Test
    fun `backoff resets to the first step after a success`() = runTest {
        val gaps = fetchGaps(
            results = listOf(
                Result.failure(Exception("1")),
                Result.failure(Exception("2")),
                Result.success(3),
                Result.failure(Exception("4"))
            ),
            runForMillis = 25_000
        )

        assertTrue(gaps[0] in 1_000L..1_200L)
        assertTrue(gaps[1] in 2_000L..2_400L)
        assertEquals("a success waits for the normal interval", interval, gaps[2])
        assertTrue("then the failure starts from the first step again", gaps[3] in 1_000L..1_200L)
    }

    @Test
    fun `a refresh request fetches immediately instead of waiting for the interval`() = runTest {
        val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        var calls = 0
        poll(refresh) { Result.success(++calls) }.test {
            assertEquals(1, awaitItem().data)
            refresh.tryEmit(Unit)
            assertEquals(2, awaitItem().data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stops fetching once the collector is cancelled`() = runTest {
        var calls = 0
        poll { Result.success(++calls) }.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        advanceTimeBy(interval * 4)
        assertEquals(1, calls)
    }

    @Test
    fun `stored data is emitted first as not live, then replaced by the first live result`() = runTest {
        pollFlow(interval, emptyFlow(), initial = "stored", backoffMillis = backoff, random = Random(0)) {
            Result.success("live")
        }.test {
            val cached = awaitItem()
            assertEquals("stored", cached.data)
            assertTrue(cached.isCached)
            assertFalse(cached.isLive)
            val live = awaitItem()
            assertEquals("live", live.data)
            assertTrue(live.isLive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when the first fetch fails the stored data stays and is reported with the error`() = runTest {
        pollFlow<String>(interval, emptyFlow(), initial = "stored", backoffMillis = backoff, random = Random(0)) {
            Result.failure(IllegalStateException("down"))
        }.test {
            awaitItem() // stored
            val failed = awaitItem()
            assertEquals("stored", failed.data)
            assertEquals("down", failed.error?.message)
            assertFalse(failed.isLive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits the successful value instance unchanged`() = runTest {
        val value = Any()
        poll { Result.success(value) }.test {
            assertSame(value, awaitItem().data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `starting offline makes no requests including manual refresh until reconnect`() = runTest {
        val online = MutableStateFlow(false)
        val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        var calls = 0
        pollFlow(interval, refresh, online = online) { Result.success(++calls) }.test {
            assertTrue(awaitItem().error?.isOffline() == true)
            refresh.tryEmit(Unit)
            advanceTimeBy(interval * 4)
            runCurrent()
            assertEquals(0, calls)
            online.value = true
            assertTrue(awaitItem().isCached)
            assertEquals(1, awaitItem().data)
            advanceTimeBy(interval)
            assertEquals(2, awaitItem().data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `disconnect marks the last result not live and reconnect fetches immediately`() = runTest {
        val online = MutableStateFlow(true)
        var calls = 0
        pollFlow(interval, emptyFlow(), online = online) { Result.success(++calls) }.test {
            assertTrue(awaitItem().isLive)
            online.value = false
            val offline = awaitItem()
            assertEquals(1, offline.data)
            assertFalse(offline.isLive)
            advanceTimeBy(interval * 4)
            runCurrent()
            assertEquals(1, calls)
            expectMostRecentItem()
            online.value = true
            assertTrue(awaitItem().isCached)
            assertEquals(2, awaitItem().data)
            advanceTimeBy(interval - 1)
            expectNoEvents()
            advanceTimeBy(1)
            assertEquals(3, awaitItem().data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `going offline cancels the current request and stale results cannot arrive later`() = runTest {
        val online = MutableStateFlow(true)
        val cancelled = CompletableDeferred<Unit>()
        var calls = 0
        pollFlow(interval, emptyFlow(), online = online) {
            calls++
            if (calls == 1) {
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            }
            Result.success(calls)
        }.test {
            runCurrent()
            assertEquals(1, calls)
            online.value = false
            assertTrue(awaitItem().error?.isOffline() == true)
            assertTrue(cancelled.isCompleted)
            online.value = true
            assertTrue(awaitItem().isCached)
            assertEquals(2, awaitItem().data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `duplicate online notifications do not reset polling and reconnect clears failure backoff`() = runTest {
        val online = MutableSharedFlow<Boolean>(replay = 1)
        online.emit(true)
        var calls = 0
        pollFlow<Int>(interval, emptyFlow(), backoffMillis = listOf(60_000), online = online) {
            calls++
            if (calls == 1) Result.failure(IllegalStateException("server")) else Result.success(calls)
        }.test {
            assertEquals("server", awaitItem().error?.message)
            online.emit(true)
            runCurrent()
            assertEquals(1, calls)
            online.emit(false)
            assertTrue(awaitItem().error?.isOffline() == true)
            online.emit(true)
            assertTrue(awaitItem().isCached)
            assertEquals(2, awaitItem().data)
            advanceTimeBy(interval)
            assertEquals(3, awaitItem().data)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
