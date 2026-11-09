package com.wedgess.luas.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.turbine.test
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.db.dao.AlarmDao
import com.wedgess.luas.data.db.luasDatabase
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmFinishReason
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmRegistration
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.AlarmVerification
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.TransportType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class AlarmConfigRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var driver: SqlDriver
    private lateinit var repository: AlarmConfigRepositoryImpl
    private val now = Instant.parse("2026-11-09T22:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
        repository = AlarmConfigRepositoryImpl(AlarmDao(luasDatabase(driver)), clock)
    }

    @After
    fun tearDown() {
        driver.close()
        context.deleteDatabase(DB_NAME)
    }

    private fun alarm(id: String, station: String = "STI", reminderOffset: Long = 600) = Alarm(
        id = id,
        requestCode = 0,
        kind = AlarmKind.SPECIFIC_DEPARTURE,
        state = AlarmState.ACTIVE,
        transport = TransportType.LUAS,
        stationCode = station,
        stationName = "Station $station",
        direction = DepartureDirection.INBOUND,
        destination = "Broombridge",
        notifyMinutesBefore = 3,
        targetExpectedAt = now.plusSeconds(reminderOffset + 210),
        targetObservedAt = now,
        reminderAt = now.plusSeconds(reminderOffset),
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun `storing the same alarm repeatedly keeps one record and its request code`() = runTest {
        val first = repository.insertIfAbsent(alarm("a")).getOrThrow()

        repeat(3) { assertEquals(first, repository.insertIfAbsent(alarm("a")).getOrThrow()) }

        assertEquals(listOf(first), repository.activeAlarms().getOrThrow())
    }

    @Test
    fun `alarms for the same station and departure coexist under their own ids`() = runTest {
        repository.insertIfAbsent(alarm("a"))
        repository.insertIfAbsent(alarm("b"))
        repository.insertIfAbsent(alarm("c", station = "STS"))

        val active = repository.activeAlarms().getOrThrow()
        assertEquals(setOf("a", "b", "c"), active.map { it.id }.toSet())
        assertEquals(3, active.map { it.requestCode }.toSet().size)
    }

    @Test
    fun `finishing is idempotent and only one of several racing callers wins`() = runTest {
        repository.insertIfAbsent(alarm("a"))

        val results = List(8) {
            async {
                repository.finish(
                    "a",
                    AlarmState.FIRED,
                    AlarmFinishReason.NOTIFIED,
                    AlarmVerification.UNVERIFIED,
                    now
                )
            }
        }.awaitAll().map { it.getOrThrow() }

        assertEquals(1, results.count { it })
        assertEquals(AlarmState.FIRED, repository.get("a").getOrThrow()!!.state)
    }

    @Test
    fun `a finished alarm is never resurrected by later updates`() = runTest {
        repository.insertIfAbsent(alarm("a"))
        repository.finish("a", AlarmState.CANCELLED, AlarmFinishReason.USER_CANCELLED)

        assertFalse(repository.updateSchedule("a", now, now, now, 1).getOrThrow())
        assertFalse(repository.updateRegistration("a", AlarmRegistration.EXACT).getOrThrow())
        assertFalse(repository.finish("a", AlarmState.FIRED, AlarmFinishReason.NOTIFIED).getOrThrow())
        assertTrue(repository.activeAlarms().getOrThrow().isEmpty())
        assertEquals(AlarmState.CANCELLED, repository.get("a").getOrThrow()!!.state)
    }

    @Test
    fun `finishing to ACTIVE is rejected`() = runTest {
        repository.insertIfAbsent(alarm("a"))

        assertTrue(repository.finish("a", AlarmState.ACTIVE, AlarmFinishReason.NOTIFIED).isFailure)
    }

    @Test
    fun `a missing alarm is reported as null rather than failing`() = runTest {
        assertNull(repository.get("missing").getOrThrow())
        assertFalse(repository.finish("missing", AlarmState.CANCELLED, AlarmFinishReason.USER_CANCELLED).getOrThrow())
    }

    @Test
    fun `observing active alarms follows creation, schedule changes and completion`() = runTest {
        repository.observeActive().test {
            assertEquals(emptyList<Alarm>(), awaitItem())
            repository.insertIfAbsent(alarm("a"))
            assertEquals(listOf("a"), awaitItem().map { it.id })
            repository.finish("a", AlarmState.CANCELLED, AlarmFinishReason.USER_CANCELLED)
            assertEquals(emptyList<Alarm>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `active alarms are restored with their progress after the database and repository are recreated`() = runTest {
        val dart = alarm("dart").copy(
            transport = TransportType.DART,
            tripCode = "E846",
            operatingDate = LocalDate.of(2026, 11, 9)
        )
        repository.insertIfAbsent(alarm("luas"))
        repository.insertIfAbsent(dart)
        repository.updateSchedule("luas", now.plusSeconds(900), now.plusSeconds(1_110), now, 2)
        repository.updateRegistration("luas", AlarmRegistration.INEXACT)
        repository.insertIfAbsent(alarm("done"))
        repository.finish("done", AlarmState.FIRED, AlarmFinishReason.NOTIFIED, AlarmVerification.LIVE_VERIFIED, now)
        driver.close()

        // Simulates process death: a new driver, DAO and repository over the same database file.
        driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
        val restored = AlarmConfigRepositoryImpl(AlarmDao(luasDatabase(driver)), clock)

        val active = restored.activeAlarms().getOrThrow()
        assertEquals(setOf("luas", "dart"), active.map { it.id }.toSet())
        val luas = active.first { it.id == "luas" }
        assertEquals(2, luas.checkCount)
        assertEquals(AlarmRegistration.INEXACT, luas.registration)
        assertEquals(now.plusSeconds(900), luas.reminderAt)
        assertEquals("E846", active.first { it.id == "dart" }.tripCode)
        assertEquals(AlarmState.FIRED, restored.get("done").getOrThrow()!!.state)
    }

    @Test
    fun `request codes keep increasing across a restart so alarms never collide`() = runTest {
        val first = repository.insertIfAbsent(alarm("a")).getOrThrow()
        driver.close()
        driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
        val restored = AlarmConfigRepositoryImpl(AlarmDao(luasDatabase(driver)), clock)

        val second = restored.insertIfAbsent(alarm("b")).getOrThrow()

        assertEquals(first.requestCode + 1, second.requestCode)
    }

    @Test
    fun `purging removes old finished alarms only`() = runTest {
        repository.insertIfAbsent(alarm("active"))
        repository.insertIfAbsent(alarm("old"))
        repository.finish("old", AlarmState.CANCELLED, AlarmFinishReason.USER_CANCELLED)

        repository.purgeFinished(updatedBeforeMillis = now.toEpochMilli() + 1)

        assertEquals(listOf("active"), listOf("active", "old").filter { repository.get(it).getOrThrow() != null })
    }

    private companion object {
        const val DB_NAME = "alarm-restore-test.db"
    }
}
