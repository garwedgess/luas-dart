package com.wedgess.luas.domain.alarm

import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmFinishReason
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmRegistration
import com.wedgess.luas.domain.model.AlarmRequest
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.AlarmVerification
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.repository.LuasRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class AlarmCoordinatorTest {

    private val t0 = Instant.parse("2026-11-09T22:00:00Z")
    private val date = LocalDate.of(2026, 11, 9)
    private val clock = MutableClock(t0)
    private val alarms = FakeAlarmConfigRepository(clock)
    private val scheduler = FakeAlarmScheduler()
    private val notifier = FakeAlarmNotifier()

    private var luasLive: Result<Departures> = Result.failure(IOException("offline"))
    private var dartLive: Result<Departures> = Result.failure(IOException("offline"))
    private var fetchDelayMillis = 0L
    private var fetches = 0

    private val luas = mockk<LuasRepository>().also { repo ->
        coEvery { repo.fetchForecast(any()) } coAnswers {
            fetches++
            delay(fetchDelayMillis)
            luasLive
        }
    }
    private val dart = mockk<DartRepository>().also { repo ->
        coEvery { repo.fetchForecast(any()) } coAnswers {
            fetches++
            delay(fetchDelayMillis)
            dartLive
        }
    }

    private fun coordinator() = AlarmCoordinator(alarms, scheduler, notifier, luas, dart, clock)
    private val coordinator = coordinator()

    private fun at(minutes: Long, seconds: Long = 0) = t0.plus(Duration.ofMinutes(minutes)).plusSeconds(seconds)

    private fun luasDeparture(
        expectedAt: Instant,
        destination: String = "Broombridge",
        direction: DepartureDirection = DepartureDirection.INBOUND
    ) = Departure(TransportType.LUAS, direction, destination, 0, expectedAt)

    private fun dartDeparture(trip: String, expectedAt: Instant, operatingDate: LocalDate? = date) = Departure(
        transport = TransportType.DART,
        direction = DepartureDirection.NORTHBOUND,
        destination = "Malahide",
        dueMins = 0,
        expectedAt = expectedAt,
        tripCode = trip,
        operatingDate = operatingDate
    )

    private fun departures(vararg items: Departure) = Departures("X", "X", clock.now, "", items.toList())

    private fun luasRequest(id: String = "alarm-1", expectedInMinutes: Long = 30, lead: Int = 5) = AlarmRequest(
        requestId = id,
        kind = AlarmKind.SPECIFIC_DEPARTURE,
        transport = TransportType.LUAS,
        stationCode = "STI",
        stationName = "Stillorgan",
        direction = DepartureDirection.INBOUND,
        destination = "Broombridge",
        notifyMinutesBefore = lead,
        expectedAt = at(expectedInMinutes),
        observedAt = t0
    )

    private fun dartRequest(id: String = "dart-1", trip: String = "E846", expectedInMinutes: Long = 30) = luasRequest(
        id = id,
        expectedInMinutes = expectedInMinutes
    ).copy(
        transport = TransportType.DART,
        stationCode = "MHIDE",
        stationName = "Malahide",
        direction = DepartureDirection.NORTHBOUND,
        destination = "Malahide",
        tripCode = trip,
        operatingDate = date
    )

    private suspend fun stored(id: String) = alarms.get(id).getOrThrow()!!

    // ---- creation: persisted first, distinct identity, exact or inexact registration ----

    @Test
    fun `creating an alarm stores it as active and registers an early live re-check before the reminder`() = runTest {
        val alarm = coordinator.create(luasRequest()).getOrThrow()

        // Expected t+30, lead 5 min, 30s safety: reminder t+24:30, early re-check 5 minutes before that.
        assertEquals(at(24, 30), alarm.reminderAt)
        assertEquals(AlarmState.ACTIVE, stored("alarm-1").state)
        assertEquals(at(19, 30), scheduler.registrations.getValue("alarm-1").first)
        assertEquals(AlarmRegistration.EXACT, stored("alarm-1").registration)
    }

    @Test
    fun `without exact alarm permission the alarm is still registered, inexactly, and says so`() = runTest {
        scheduler.exactAllowed = false

        coordinator.create(luasRequest())

        assertEquals(AlarmRegistration.INEXACT, stored("alarm-1").registration)
        assertEquals(AlarmRegistration.INEXACT, scheduler.registrations.getValue("alarm-1").second)
    }

    @Test
    fun `a reminder too close for a re-check is registered directly as the delivery`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 8, lead = 5))

        assertEquals(at(2, 30), scheduler.registrations.getValue("alarm-1").first)
    }

    @Test
    fun `a reminder already due is registered just after now`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 5, lead = 5))

        assertEquals(t0.plusSeconds(2), scheduler.registrations.getValue("alarm-1").first)
    }

    @Test
    fun `repeating a creation request is idempotent`() = runTest {
        val first = coordinator.create(luasRequest()).getOrThrow()
        val second = coordinator.create(luasRequest()).getOrThrow()

        assertEquals(1, alarms.all.size)
        assertEquals(first.requestCode, second.requestCode)
        assertEquals(1, scheduler.registrations.size)
    }

    @Test
    fun `alarms for the same station, direction and train are independent`() = runTest {
        coordinator.create(luasRequest(id = "a"))
        coordinator.create(luasRequest(id = "b", lead = 10))
        coordinator.create(dartRequest(id = "c", trip = "E846"))
        coordinator.create(dartRequest(id = "d", trip = "E846"))

        assertEquals(4, alarms.all.size)
        assertEquals(4, alarms.all.map { it.requestCode }.toSet().size)
        assertEquals(setOf("a", "b", "c", "d"), scheduler.registrations.keys)
    }

    @Test
    fun `cancelling one alarm leaves the others registered`() = runTest {
        coordinator.create(luasRequest(id = "a"))
        coordinator.create(luasRequest(id = "b"))

        coordinator.cancel("a")

        assertEquals(setOf("b"), scheduler.registrations.keys)
        assertEquals(AlarmState.ACTIVE, stored("b").state)
    }

    @Test
    fun `a failed OS registration ends the alarm instead of leaving it active`() = runTest {
        scheduler.failWith = IllegalStateException("too many alarms")

        val result = coordinator.create(luasRequest())

        assertTrue(result.isFailure)
        val alarm = stored("alarm-1")
        assertEquals(AlarmState.EXPIRED, alarm.state)
        assertEquals(AlarmFinishReason.REGISTRATION_FAILED, alarm.finishReason)
    }

    @Test
    fun `an invalid lead time is rejected before anything is stored`() = runTest {
        assertTrue(coordinator.create(luasRequest(lead = 0)).isFailure)
        assertTrue(alarms.all.isEmpty())
    }

    // ---- cancellation, completion and duplicate broadcasts ----

    @Test
    fun `cancelling moves the alarm to cancelled, clears the OS side and dismisses the notification`() = runTest {
        val alarm = coordinator.create(luasRequest()).getOrThrow()

        assertTrue(coordinator.cancel("alarm-1").getOrThrow())

        assertEquals(AlarmState.CANCELLED, stored("alarm-1").state)
        assertEquals(AlarmFinishReason.USER_CANCELLED, stored("alarm-1").finishReason)
        assertTrue(scheduler.registrations.isEmpty())
        assertEquals(listOf(alarm.requestCode), notifier.dismissed)
    }

    @Test
    fun `cancelling twice or cancelling an unknown alarm is harmless`() = runTest {
        coordinator.create(luasRequest())

        assertTrue(coordinator.cancel("alarm-1").getOrThrow())
        assertFalse(coordinator.cancel("alarm-1").getOrThrow())
        assertFalse(coordinator.cancel("missing").getOrThrow())
    }

    @Test
    fun `a wake-up that arrives after cancellation does nothing`() = runTest {
        coordinator.create(luasRequest())
        coordinator.cancel("alarm-1")
        clock.now = at(25)
        luasLive = Result.success(departures(luasDeparture(at(30))))

        coordinator.onAlarmDue("alarm-1")

        assertTrue(notifier.posted.isEmpty())
        assertEquals(AlarmState.CANCELLED, stored("alarm-1").state)
        assertEquals(0, fetches)
    }

    @Test
    fun `cancelling cannot be undone by restoration`() = runTest {
        coordinator.create(luasRequest())
        coordinator.cancel("alarm-1")

        coordinator().reconcile()

        assertEquals(AlarmState.CANCELLED, stored("alarm-1").state)
        assertTrue(scheduler.registrations.isEmpty())
    }

    @Test
    fun `an unknown alarm id is ignored`() = runTest {
        coordinator.onAlarmDue("missing")

        assertTrue(notifier.posted.isEmpty())
    }

    @Test
    fun `repeated broadcasts for the same alarm notify once`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 8))
        clock.now = at(2, 30)
        luasLive = Result.success(departures(luasDeparture(at(8))))

        repeat(3) { coordinator.onAlarmDue("alarm-1") }

        assertEquals(1, notifier.posted.size)
        assertEquals(AlarmState.FIRED, stored("alarm-1").state)
    }

    @Test
    fun `broadcasts racing each other still produce one notification`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 8))
        clock.now = at(2, 30)
        luasLive = Result.success(departures(luasDeparture(at(8))))
        fetchDelayMillis = 500

        List(4) { async { coordinator.onAlarmDue("alarm-1") } }.awaitAll()

        assertEquals(1, notifier.posted.size)
    }

    @Test
    fun `a cancellation racing the delivery cannot also notify`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 8))
        clock.now = at(2, 30)
        luasLive = Result.success(departures(luasDeparture(at(8))))
        fetchDelayMillis = 500

        val delivery = async { coordinator.onAlarmDue("alarm-1") }
        val cancel = async { coordinator.cancel("alarm-1") }
        listOf(delivery, cancel).awaitAll()

        // Whichever won, the alarm ends in exactly one terminal state and cannot have both notified and cancelled.
        val state = stored("alarm-1").state
        assertTrue(state == AlarmState.CANCELLED || state == AlarmState.FIRED)
        assertEquals(if (state == AlarmState.FIRED) 1 else 0, notifier.posted.size)
    }

    @Test
    fun `cancelling during registration drops the registration that was just made`() = runTest {
        // The alarm is cancelled by another path between its insert and its OS registration completing.
        val racing = object : AlarmScheduler by scheduler {
            override fun register(alarm: Alarm, triggerAt: Instant): AlarmRegistration {
                val result = scheduler.register(alarm, triggerAt)
                kotlinx.coroutines.runBlocking {
                    alarms.finish(
                        alarm.id,
                        AlarmState.CANCELLED,
                        AlarmFinishReason.USER_CANCELLED
                    )
                }
                return result
            }
        }
        AlarmCoordinator(alarms, racing, notifier, luas, dart, clock).create(luasRequest())

        assertTrue("no orphaned registration remains", scheduler.registrations.isEmpty())
    }

    // ---- live re-check (before the reminder) ----

    @Test
    fun `an early re-check with live data confirms the target and schedules only the delivery`() = runTest {
        coordinator.create(luasRequest())
        clock.now = at(19, 30)
        luasLive = Result.success(departures(luasDeparture(at(30))))

        coordinator.onAlarmDue("alarm-1")

        val alarm = stored("alarm-1")
        assertEquals(1, alarm.checkCount)
        assertEquals(clock.now, alarm.targetObservedAt)
        assertEquals(at(24, 30), scheduler.registrations.getValue("alarm-1").first)
        assertTrue(notifier.posted.isEmpty())
    }

    @Test
    fun `a delayed service moves the reminder later and reschedules`() = runTest {
        coordinator.create(luasRequest())
        clock.now = at(19, 30)
        luasLive = Result.success(departures(luasDeparture(at(36)))) // six minutes late

        coordinator.onAlarmDue("alarm-1")

        val alarm = stored("alarm-1")
        assertEquals(at(30, 30), alarm.reminderAt)
        assertEquals(at(36), alarm.targetExpectedAt)
        assertEquals(at(25, 30), scheduler.registrations.getValue("alarm-1").first) // next early re-check
    }

    @Test
    fun `a service running early brings the reminder forward and delivers immediately when already due`() = runTest {
        coordinator.create(luasRequest())
        clock.now = at(19, 30)
        luasLive = Result.success(departures(luasDeparture(at(25)))) // five minutes early

        coordinator.onAlarmDue("alarm-1")

        assertEquals(1, notifier.posted.size)
        val (_, content) = notifier.posted.single()
        content as ReminderContent.Upcoming
        assertEquals(AlarmVerification.LIVE_VERIFIED, content.verification)
        assertEquals(at(25), content.expectedAt)
    }

    @Test
    fun `a failed re-check is not retried and the delivery stays scheduled`() = runTest {
        coordinator.create(luasRequest())
        clock.now = at(19, 30)

        coordinator.onAlarmDue("alarm-1")

        assertEquals(1, fetches)
        assertEquals(1, stored("alarm-1").checkCount)
        assertEquals(at(24, 30), stored("alarm-1").reminderAt)
        assertEquals(at(24, 30), scheduler.registrations.getValue("alarm-1").first)
        assertTrue(notifier.posted.isEmpty())
    }

    @Test
    fun `a live request that outlasts the timeout is treated like a network failure`() = runTest {
        coordinator.create(luasRequest())
        clock.now = at(19, 30)
        luasLive = Result.success(departures(luasDeparture(at(40))))
        fetchDelayMillis = LiveTimeout.toMillis() + 5_000

        coordinator.onAlarmDue("alarm-1")

        // The late answer is ignored: the reminder time was not moved.
        assertEquals(at(24, 30), stored("alarm-1").reminderAt)
    }

    @Test
    fun `an early re-check that finds the target missing keeps the plan and leaves the verdict to delivery`() = runTest {
        coordinator.create(luasRequest())
        clock.now = at(19, 30)
        luasLive = Result.success(departures())

        coordinator.onAlarmDue("alarm-1")

        assertTrue(notifier.posted.isEmpty())
        assertEquals(AlarmState.ACTIVE, stored("alarm-1").state)
        assertEquals(at(24, 30), scheduler.registrations.getValue("alarm-1").first)
    }

    // ---- delivery: live verified, offline, late, permissions ----

    @Test
    fun `delivery confirmed by live data is reported as live-verified`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 8))
        clock.now = at(2, 30)
        luasLive = Result.success(departures(luasDeparture(at(8))))

        coordinator.onAlarmDue("alarm-1")

        val alarm = stored("alarm-1")
        assertEquals(AlarmState.FIRED, alarm.state)
        assertEquals(AlarmFinishReason.NOTIFIED, alarm.finishReason)
        assertEquals(AlarmVerification.LIVE_VERIFIED, alarm.verification)
        assertEquals(clock.now, alarm.notifiedAt)
        assertTrue(scheduler.registrations.isEmpty())
    }

    @Test
    fun `offline delivery still reminds but is never represented as live-verified`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 8))
        clock.now = at(2, 30)

        coordinator.onAlarmDue("alarm-1")

        val content = notifier.posted.single().second as ReminderContent.Upcoming
        assertEquals(AlarmVerification.UNVERIFIED, content.verification)
        assertEquals(at(8), content.expectedAt)
        assertEquals(AlarmVerification.UNVERIFIED, stored("alarm-1").verification)
    }

    @Test
    fun `delivery delayed by Doze but before the service is due still reminds`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 30))
        clock.now = at(27) // the 24:30 reminder was delivered 2.5 minutes late

        coordinator.onAlarmDue("alarm-1")

        val content = notifier.posted.single().second as ReminderContent.Upcoming
        assertEquals(at(30), content.expectedAt)
        assertEquals(AlarmState.FIRED, stored("alarm-1").state)
    }

    @Test
    fun `a reminder delivered after the service was due is reported as missed, not as upcoming`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 30))
        clock.now = at(33)

        coordinator.onAlarmDue("alarm-1")

        assertTrue(notifier.posted.single().second is ReminderContent.Missed)
        val alarm = stored("alarm-1")
        assertEquals(AlarmState.EXPIRED, alarm.state)
        assertEquals(AlarmFinishReason.MISSED, alarm.finishReason)
    }

    @Test
    fun `without notification permission the alarm completes without showing anything`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 8))
        notifier.enabled = false
        clock.now = at(2, 30)

        coordinator.onAlarmDue("alarm-1")

        assertTrue(notifier.posted.isEmpty())
        assertEquals(AlarmFinishReason.NOTIFICATIONS_DISABLED, stored("alarm-1").finishReason)
        assertEquals(AlarmState.FIRED, stored("alarm-1").state)
    }

    @Test
    fun `exact alarm permission and notification permission are independent`() = runTest {
        scheduler.exactAllowed = false
        notifier.enabled = true
        assertEquals(false, coordinator.capabilities.exactAlarms)
        assertEquals(true, coordinator.capabilities.notifications)

        scheduler.exactAllowed = true
        notifier.enabled = false
        assertEquals(true, coordinator.capabilities.exactAlarms)
        assertEquals(false, coordinator.capabilities.notifications)
    }

    @Test
    fun `a delivery that finds the service later than planned waits while re-checks remain`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 8))
        clock.now = at(2, 30)
        luasLive = Result.success(departures(luasDeparture(at(11)))) // three minutes late

        coordinator.onAlarmDue("alarm-1")

        assertTrue(notifier.posted.isEmpty())
        assertEquals(at(5, 30), stored("alarm-1").reminderAt)
        assertEquals(1, stored("alarm-1").checkCount)
    }

    @Test
    fun `once re-checks are used up a late service no longer defers the reminder`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 8))
        alarms.updateSchedule("alarm-1", at(2, 30), at(8), t0, Alarm.MAX_LIVE_CHECKS)
        clock.now = at(2, 30)
        luasLive = Result.success(departures(luasDeparture(at(11))))

        coordinator.onAlarmDue("alarm-1")

        assertEquals(1, notifier.posted.size)
    }

    // ---- DART: train code + operating date, separate from alarm id ----

    @Test
    fun `a DART alarm follows its train by code through delays`() = runTest {
        coordinator.create(dartRequest(trip = "E846"))
        clock.now = at(19, 30)
        dartLive = Result.success(departures(dartDeparture("E111", at(31)), dartDeparture("E846", at(38))))

        coordinator.onAlarmDue("dart-1")

        assertEquals(at(38), stored("dart-1").targetExpectedAt)
        assertEquals("E846", stored("dart-1").tripCode)
        assertEquals("dart-1", stored("dart-1").id)
    }

    @Test
    fun `a DART train missing from live results when the reminder is due is reported, not silently dropped`() = runTest {
        coordinator.create(dartRequest(expectedInMinutes = 8))
        clock.now = at(2, 30)
        dartLive = Result.success(departures(dartDeparture("E111", at(9))))

        coordinator.onAlarmDue("dart-1")

        assertEquals(ReminderContent.ServiceLost, notifier.posted.single().second)
        assertEquals(AlarmFinishReason.SERVICE_LOST, stored("dart-1").finishReason)
    }

    @Test
    fun `the same DART train code on another operating date is not the alarm's train`() = runTest {
        coordinator.create(dartRequest(expectedInMinutes = 8))
        clock.now = at(2, 30)
        dartLive = Result.success(departures(dartDeparture("E846", at(8), operatingDate = date.plusDays(1))))

        coordinator.onAlarmDue("dart-1")

        assertEquals(ReminderContent.ServiceLost, notifier.posted.single().second)
    }

    @Test
    fun `a DART train that has just left the list near its time still delivers an unverified reminder`() = runTest {
        coordinator.create(dartRequest(expectedInMinutes = 8))
        clock.now = at(7, 30) // 30 s before the expected time
        dartLive = Result.success(departures())

        coordinator.onAlarmDue("dart-1")

        val content = notifier.posted.single().second as ReminderContent.Upcoming
        assertEquals(AlarmVerification.UNVERIFIED, content.verification)
    }

    // ---- Luas: no vehicle identity ----

    @Test
    fun `an ambiguous Luas match is flagged on the reminder`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 8))
        clock.now = at(2, 30)
        luasLive = Result.success(departures(luasDeparture(at(8)), luasDeparture(at(10))))

        coordinator.onAlarmDue("alarm-1")

        assertTrue((notifier.posted.single().second as ReminderContent.Upcoming).ambiguous)
    }

    @Test
    fun `a Luas alarm never switches to a tram with a different destination`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 8))
        clock.now = at(2, 30)
        luasLive = Result.success(departures(luasDeparture(at(8), destination = "Parnell")))

        coordinator.onAlarmDue("alarm-1")

        assertEquals(ReminderContent.ServiceLost, notifier.posted.single().second)
        assertEquals(at(8), stored("alarm-1").targetExpectedAt)
    }

    // ---- restoration ----

    @Test
    fun `restoring after a reboot re-registers active alarms from the database`() = runTest {
        coordinator.create(luasRequest(id = "a"))
        coordinator.create(dartRequest(id = "b"))
        scheduler.registrations.clear() // the OS lost every registration

        coordinator().reconcile()

        assertEquals(setOf("a", "b"), scheduler.registrations.keys)
        assertEquals(at(19, 30), scheduler.registrations.getValue("a").first)
    }

    @Test
    fun `restoring is idempotent and never notifies or duplicates`() = runTest {
        coordinator.create(luasRequest())

        repeat(3) { coordinator().reconcile() }

        assertEquals(1, alarms.all.size)
        assertEquals(1, scheduler.registrations.size)
        assertTrue(notifier.posted.isEmpty())
    }

    @Test
    fun `restoration does not resurrect finished alarms`() = runTest {
        coordinator.create(luasRequest(id = "done", expectedInMinutes = 8))
        clock.now = at(2, 30)
        coordinator.onAlarmDue("done")
        coordinator.create(luasRequest(id = "cancelled"))
        coordinator.cancel("cancelled")
        scheduler.registrations.clear()
        notifier.posted.clear()

        coordinator().reconcile()

        assertTrue(scheduler.registrations.isEmpty())
        assertTrue(notifier.posted.isEmpty())
    }

    @Test
    fun `an alarm that became overdue while the device was off is delivered on restoration`() = runTest {
        coordinator.create(luasRequest(expectedInMinutes = 30))
        scheduler.registrations.clear()
        clock.now = at(26) // booted after the reminder time but before the service

        coordinator().reconcile()

        assertEquals(1, notifier.posted.size)
        assertEquals(AlarmState.FIRED, stored("alarm-1").state)
    }

    @Test
    fun `restoring after process recreation keeps each alarm's progress`() = runTest {
        coordinator.create(luasRequest())
        clock.now = at(19, 30)
        luasLive = Result.success(departures(luasDeparture(at(33))))
        coordinator.onAlarmDue("alarm-1")
        val before = stored("alarm-1")
        scheduler.registrations.clear()

        coordinator().reconcile() // new process, same database

        assertEquals(before.checkCount, stored("alarm-1").checkCount)
        assertEquals(before.targetExpectedAt, stored("alarm-1").targetExpectedAt)
        assertEquals(1, scheduler.registrations.size)
    }

    @Test
    fun `a permission change is handled by re-registering with the new capability`() = runTest {
        coordinator.create(luasRequest())
        assertEquals(AlarmRegistration.EXACT, stored("alarm-1").registration)

        scheduler.exactAllowed = false
        coordinator.reconcile()
        assertEquals(AlarmRegistration.INEXACT, stored("alarm-1").registration)

        scheduler.exactAllowed = true
        coordinator.reconcile()
        assertEquals(AlarmRegistration.EXACT, stored("alarm-1").registration)
    }

    @Test
    fun `one alarm failing to restore does not stop the others`() = runTest {
        coordinator.create(luasRequest(id = "a"))
        coordinator.create(luasRequest(id = "b"))
        scheduler.registrations.clear()
        var failFirst = true
        val flaky = object : AlarmScheduler by scheduler {
            override fun register(alarm: Alarm, triggerAt: Instant): AlarmRegistration {
                if (failFirst) {
                    failFirst = false
                    error("boom")
                }
                return scheduler.register(alarm, triggerAt)
            }
        }

        AlarmCoordinator(alarms, flaky, notifier, luas, dart, clock).reconcile()

        assertEquals(1, scheduler.registrations.size)
    }

    @Test
    fun `old finished alarms are purged but active ones are kept`() = runTest {
        coordinator.create(luasRequest(id = "old"))
        coordinator.cancel("old")
        coordinator.create(luasRequest(id = "live", expectedInMinutes = 15 * 24 * 60L))
        clock.now = t0.plus(Duration.ofDays(8))

        coordinator().reconcile()

        assertNull(alarms.get("old").getOrThrow())
        assertEquals(AlarmState.ACTIVE, stored("live").state)
    }

    // ---- bounded behaviour ----

    @Test
    fun `no network and a permanently delayed service still terminates within a bounded number of wake-ups`() = runTest {
        coordinator.create(luasRequest())
        var wakeUps = 0
        while (true) {
            val trigger = scheduler.registrations["alarm-1"]?.first ?: break
            clock.now = trigger
            // Live data always claims the service is another 10 minutes away, so it never settles.
            luasLive = Result.success(departures(luasDeparture(clock.now.plus(Duration.ofMinutes(10 + 5)))))
            coordinator.onAlarmDue("alarm-1")
            wakeUps++
            check(wakeUps < 20) { "alarm chain did not terminate" }
        }

        assertTrue("wake-ups ($wakeUps) must stay bounded", wakeUps <= Alarm.MAX_LIVE_CHECKS + 2)
        assertEquals(AlarmState.FIRED, stored("alarm-1").state)
        assertEquals(1, notifier.posted.size)
    }

    @Test
    fun `with the network down the alarm costs one early attempt and one delivery`() = runTest {
        coordinator.create(luasRequest())
        var wakeUps = 0
        while (true) {
            val trigger = scheduler.registrations["alarm-1"]?.first ?: break
            clock.now = trigger
            coordinator.onAlarmDue("alarm-1")
            wakeUps++
            check(wakeUps < 20)
        }

        assertEquals(2, wakeUps)
        assertEquals(2, fetches)
        assertEquals(AlarmVerification.UNVERIFIED, stored("alarm-1").verification)
    }

    @Test
    fun `dismissing a shown notification removes it without changing the alarm`() = runTest {
        val alarm = coordinator.create(luasRequest(expectedInMinutes = 8)).getOrThrow()
        clock.now = at(2, 30)
        coordinator.onAlarmDue("alarm-1")

        coordinator.dismissNotification("alarm-1")

        assertEquals(listOf(alarm.requestCode), notifier.dismissed)
        assertEquals(AlarmState.FIRED, stored("alarm-1").state)
    }
}
