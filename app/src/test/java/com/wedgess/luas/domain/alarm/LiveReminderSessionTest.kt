package com.wedgess.luas.domain.alarm

import app.cash.turbine.test
import com.wedgess.luas.domain.NetworkMonitor
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.AlarmVerification
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.repository.LuasRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class LiveReminderSessionTest {
    private val now = Instant.parse("2026-11-11T09:00:00Z")
    private val clock = MutableClock(now)
    private val online = MutableStateFlow(true)
    private val network = object : NetworkMonitor {
        override val isOnline = online
    }
    private val luas = mockk<LuasRepository>()
    private val dart = mockk<DartRepository>()
    private val session = LiveReminderSession(luas, dart, network, clock)
    private val date = LocalDate.of(2026, 11, 11)
    private val departure = Departure(
        TransportType.LUAS,
        DepartureDirection.INBOUND,
        "Broombridge",
        5,
        now.plusSeconds(300)
    )
    private val alarm = Alarm(
        id = "a", requestCode = 1, kind = AlarmKind.SPECIFIC_DEPARTURE, state = AlarmState.FIRED,
        transport = TransportType.LUAS, stationCode = "PHI", stationName = "Phibsborough",
        direction = DepartureDirection.INBOUND, destination = "Broombridge", notifyMinutesBefore = 3,
        targetExpectedAt = departure.expectedAt, targetObservedAt = now,
        reminderAt = now, notifiedAt = now, createdAt = now, updatedAt = now
    )
    private val initial = ReminderContent.Upcoming(departure.expectedAt, AlarmVerification.LIVE_VERIFIED)
    private fun forecast(vararg services: Departure) = Departures(
        "PHI",
        "Phibsborough",
        clock.now,
        "",
        services.toList()
    )

    @Test
    fun `Luas ETA changes are fetched immediately then every fifteen seconds`() = runTest {
        var live = forecast(departure)
        coEvery { luas.fetchForecast("PHI") } answers { Result.success(live) }
        session.updates(alarm, initial).test {
            assertEquals(departure.expectedAt, (awaitItem() as ReminderContent.Upcoming).expectedAt)
            live = forecast(departure.copy(expectedAt = now.plusSeconds(360)))
            clock.now = now.plusSeconds(15)
            advanceTimeBy(15_000)
            val updated = awaitItem() as ReminderContent.Upcoming
            assertEquals(now.plusSeconds(360), updated.expectedAt)
            assertEquals(clock.now, updated.lastCheckedAt)
            coVerify(exactly = 2) { luas.fetchForecast("PHI") }
            coVerify(exactly = 0) { dart.fetchForecast(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `DART follows its train and operating date at the forty second interval`() = runTest {
        val train = departure.copy(transport = TransportType.DART, tripCode = "E134", operatingDate = date)
        val selected = alarm.copy(transport = TransportType.DART, tripCode = "E134", operatingDate = date)
        var live = forecast(train)
        coEvery { dart.fetchForecast("PHI") } answers { Result.success(live) }
        session.updates(selected, initial).test {
            awaitItem()
            live = forecast(train.copy(expectedAt = now.plusSeconds(420)))
            clock.now = now.plusSeconds(40)
            advanceTimeBy(39_999)
            expectNoEvents()
            advanceTimeBy(1)
            assertEquals(now.plusSeconds(420), (awaitItem() as ReminderContent.Upcoming).expectedAt)
            coVerify(exactly = 2) { dart.fetchForecast("PHI") }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `offline pauses network calls and reconnect refreshes immediately`() = runTest {
        coEvery { luas.fetchForecast("PHI") } answers { Result.success(forecast(departure)) }
        session.updates(alarm, initial).test {
            awaitItem()
            online.value = false
            assertEquals(AlarmVerification.UNVERIFIED, (awaitItem() as ReminderContent.Upcoming).verification)
            clock.now = now.plusSeconds(60)
            advanceTimeBy(60_000)
            runCurrent()
            coVerify(exactly = 1) { luas.fetchForecast("PHI") }
            online.value = true
            assertEquals(AlarmVerification.LIVE_VERIFIED, (awaitItem() as ReminderContent.Upcoming).verification)
            coVerify(exactly = 2) { luas.fetchForecast("PHI") }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed refresh disables live status and preserves the last checked arrival`() = runTest {
        var live = Result.success(forecast(departure))
        coEvery { luas.fetchForecast("PHI") } answers { live }
        session.updates(alarm, initial).test {
            awaitItem()
            live = Result.failure(IOException("offline"))
            clock.now = now.plusSeconds(15)
            advanceTimeBy(15_000)
            val paused = awaitItem() as ReminderContent.Upcoming
            assertEquals(AlarmVerification.UNVERIFIED, paused.verification)
            assertEquals(departure.expectedAt, paused.expectedAt)
            assertEquals(now, paused.lastCheckedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a missing tram is not silently replaced by a later tram to the same destination`() = runTest {
        var live = forecast(departure)
        coEvery { luas.fetchForecast("PHI") } answers { Result.success(live) }
        session.updates(alarm, initial).test {
            awaitItem()
            live = forecast(departure.copy(expectedAt = now.plusSeconds(1_020)))
            clock.now = now.plusSeconds(15)
            advanceTimeBy(15_000)
            assertEquals(ReminderContent.ServiceLost, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `the same DART code on another operating date is not followed`() = runTest {
        val train = departure.copy(transport = TransportType.DART, tripCode = "E134", operatingDate = date.plusDays(1))
        coEvery { dart.fetchForecast("PHI") } returns Result.success(forecast(train))
        session.updates(
            alarm.copy(transport = TransportType.DART, tripCode = "E134", operatingDate = date),
            initial
        ).test {
            // The first clock tick may precede the response.
            var result = awaitItem()
            if (result is ReminderContent.Upcoming) result = awaitItem()
            assertEquals(ReminderContent.ServiceLost, result)
            awaitComplete()
        }
    }

    @Test
    fun `cancelling tracking cancels its in flight request and stops further polling`() = runTest {
        val cancelled = CompletableDeferred<Unit>()
        coEvery { luas.fetchForecast("PHI") } coAnswers {
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        session.updates(alarm, initial).test {
            awaitItem()
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }
        runCurrent()
        assertTrue(cancelled.isCompleted)
        advanceTimeBy(120_000)
        coVerify(exactly = 1) { luas.fetchForecast("PHI") }
    }

    @Test
    fun `an offline session ends after the last expected arrival plus its grace period`() = runTest {
        online.value = false
        session.updates(alarm, initial.copy(verification = AlarmVerification.UNVERIFIED)).test {
            assertTrue(awaitItem() is ReminderContent.Upcoming)
            clock.now = now.plusSeconds(361)
            advanceTimeBy(1_000)
            assertEquals(ReminderContent.TrackingFinished(departure.expectedAt), awaitItem())
            awaitComplete()
            coVerify(exactly = 0) { luas.fetchForecast(any()) }
        }
    }

    @Test
    fun `tracking has a thirty minute limit even when the service is far in the future`() = runTest {
        online.value = false
        val future = initial.copy(expectedAt = now.plusSeconds(5_400), verification = AlarmVerification.UNVERIFIED)
        session.updates(alarm, future).test {
            awaitItem()
            clock.now = now.plusSeconds(1_801)
            advanceTimeBy(1_000)
            assertEquals(ReminderContent.TrackingFinished(future.expectedAt), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `reaching zero disables the ticking countdown before the session grace period expires`() = runTest {
        coEvery { luas.fetchForecast("PHI") } answers { Result.success(forecast(departure)) }
        session.updates(alarm, initial).test {
            assertFalse((awaitItem() as ReminderContent.Upcoming).countdownFinished)
            clock.now = requireNotNull(departure.expectedAt)
            advanceTimeBy(1_000)
            assertTrue((awaitItem() as ReminderContent.Upcoming).countdownFinished)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a slow successful DART refresh is not cut off by the alarm receivers delivery timeout`() = runTest {
        val train = departure.copy(transport = TransportType.DART, tripCode = "E134", operatingDate = date)
        coEvery { dart.fetchForecast("PHI") } coAnswers {
            delay(12_000)
            Result.success(forecast(train))
        }
        session.updates(
            alarm.copy(transport = TransportType.DART, tripCode = "E134", operatingDate = date),
            initial.copy(verification = AlarmVerification.UNVERIFIED)
        ).test {
            awaitItem()
            clock.now = now.plusSeconds(12)
            advanceTimeBy(12_000)
            val updated = awaitItem() as ReminderContent.Upcoming
            assertEquals(AlarmVerification.LIVE_VERIFIED, updated.verification)
            assertEquals(clock.now, updated.lastCheckedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an empty DART result has no station code and ends tracking instead of claiming network failure`() = runTest {
        coEvery { dart.fetchForecast("PHI") } returns Result.success(forecast().copy(stationCode = ""))
        session.updates(alarm.copy(transport = TransportType.DART), initial).test {
            var result = awaitItem()
            if (result is ReminderContent.Upcoming) result = awaitItem()
            assertEquals(ReminderContent.ServiceLost, result)
            awaitComplete()
        }
    }

    @Test
    fun `reminders sharing a station poll once and stop requests after their last subscriber leaves`() = runTest {
        coEvery { luas.fetchForecast("PHI") } answers { Result.success(forecast(departure)) }
        val shared = session.forecasts(alarm).shareIn(
            backgroundScope,
            SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
            replay = 1
        )
        val first = backgroundScope.launch { session.updates(alarm, initial, shared).collect() }
        val second = backgroundScope.launch { session.updates(alarm.copy(id = "b"), initial, shared).collect() }
        runCurrent()
        coVerify(exactly = 1) { luas.fetchForecast("PHI") }
        first.cancelAndJoin()
        clock.now = now.plusSeconds(15)
        advanceTimeBy(15_000)
        runCurrent()
        coVerify(exactly = 2) { luas.fetchForecast("PHI") }
        second.cancelAndJoin()
        runCurrent()
        advanceTimeBy(120_000)
        runCurrent()
        coVerify(exactly = 2) { luas.fetchForecast("PHI") }
    }
}
