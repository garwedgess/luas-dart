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
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.TransportType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

    private fun alarm(
        id: String,
        station: String = "STI",
        kind: AlarmKind = AlarmKind.NEXT_DEPARTURE,
        direction: DepartureDirection? = DepartureDirection.INBOUND
    ) = Alarm(
        id = id,
        kind = kind,
        state = AlarmState.ACTIVE,
        transport = TransportType.LUAS,
        stationCode = station,
        stationName = "Station $station",
        direction = direction,
        notifyMinutesBefore = 3,
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun `saving the same alarm repeatedly keeps one record`() = runTest {
        val alarm = alarm("a")

        repeat(3) { repository.save(alarm) }

        assertEquals(listOf(alarm), repository.activeAlarms().getOrThrow())
    }

    @Test
    fun `stable ids are deterministic and distinguish stations, directions and services`() {
        val inbound = Alarm.nextDepartureId(TransportType.LUAS, "STI", DepartureDirection.INBOUND)

        assertEquals(inbound, Alarm.nextDepartureId(TransportType.LUAS, "STI", DepartureDirection.INBOUND))
        assertNotEquals(inbound, Alarm.nextDepartureId(TransportType.LUAS, "STI", DepartureDirection.OUTBOUND))
        assertNotEquals(inbound, Alarm.nextDepartureId(TransportType.LUAS, "STS", DepartureDirection.INBOUND))
        assertNotEquals(inbound, Alarm.nextDepartureId(TransportType.DART, "STI", DepartureDirection.INBOUND))
        val date = LocalDate.of(2026, 11, 9)
        assertNotEquals(
            Alarm.departureId(TransportType.DART, "MHIDE", "E846", date),
            Alarm.departureId(TransportType.DART, "MHIDE", "E846", date.plusDays(1))
        )
    }

    @Test
    fun `alarms belong to their own stations and several can be active at once`() = runTest {
        repository.save(alarm("a", station = "STI"))
        repository.save(alarm("b", station = "STS"))

        assertEquals(setOf("STI", "STS"), repository.activeAlarms().getOrThrow().map { it.stationCode }.toSet())
    }

    @Test
    fun `finishing an alarm is idempotent`() = runTest {
        repository.save(alarm("a"))

        assertTrue(repository.finish("a", AlarmState.CANCELLED).getOrThrow())
        assertFalse(repository.finish("a", AlarmState.CANCELLED).getOrThrow())

        assertEquals(AlarmState.CANCELLED, repository.get("a").getOrThrow()!!.state)
        assertTrue(repository.activeAlarms().getOrThrow().isEmpty())
    }

    @Test
    fun `finishing an alarm to ACTIVE is rejected`() = runTest {
        repository.save(alarm("a"))

        assertTrue(repository.finish("a", AlarmState.ACTIVE).isFailure)
        assertTrue(repository.finishAllActive(AlarmState.ACTIVE).isFailure)
    }

    @Test
    fun `finishing all active alarms leaves earlier finished ones as they were`() = runTest {
        repository.save(alarm("a"))
        repository.save(alarm("b"))
        repository.finish("b", AlarmState.FIRED)

        repository.finishAllActive(AlarmState.CANCELLED)

        assertEquals(AlarmState.CANCELLED, repository.get("a").getOrThrow()!!.state)
        assertEquals(AlarmState.FIRED, repository.get("b").getOrThrow()!!.state)
    }

    @Test
    fun `a missing alarm is reported as null rather than failing`() = runTest {
        assertNull(repository.get("missing").getOrThrow())
        assertFalse(repository.finish("missing", AlarmState.CANCELLED).getOrThrow())
    }

    @Test
    fun `observing active alarms follows saves and cancellations`() = runTest {
        repository.observeActive().test {
            assertEquals(emptyList<Alarm>(), awaitItem())
            repository.save(alarm("a"))
            assertEquals(listOf("a"), awaitItem().map { it.id })
            repository.finish("a", AlarmState.CANCELLED)
            assertEquals(emptyList<Alarm>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `active alarms are restored after the database and repository are recreated`() = runTest {
        val specific = alarm("s", kind = AlarmKind.SPECIFIC_DEPARTURE).copy(
            tripCode = "E846",
            operatingDate = LocalDate.of(2026, 11, 9),
            targetExpectedAt = now.plusSeconds(600),
            targetObservedAt = now
        )
        repository.save(alarm("n"))
        repository.save(specific)
        repository.save(alarm("done"))
        repository.finish("done", AlarmState.FIRED)
        driver.close()

        // Simulates process death: a new driver, DAO and repository over the same database file.
        driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
        val restored = AlarmConfigRepositoryImpl(AlarmDao(luasDatabase(driver)), clock)

        val active = restored.activeAlarms().getOrThrow()
        assertEquals(setOf("n", "s"), active.map { it.id }.toSet())
        assertEquals(specific, active.first { it.id == "s" })
        assertEquals(AlarmState.FIRED, restored.get("done").getOrThrow()!!.state)
    }

    @Test
    fun `restoring and re-saving after a restart does not duplicate alarms`() = runTest {
        repository.save(alarm("a"))
        driver.close()
        driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
        val restored = AlarmConfigRepositoryImpl(AlarmDao(luasDatabase(driver)), clock)

        restored.activeAlarms().getOrThrow().forEach { restored.save(it) }

        assertEquals(1, restored.activeAlarms().getOrThrow().size)
    }

    @Test
    fun `purging removes old finished alarms only`() = runTest {
        repository.save(alarm("active"))
        repository.save(alarm("old"))
        repository.finish("old", AlarmState.CANCELLED)

        repository.purgeFinished(updatedBeforeMillis = now.toEpochMilli() + 1)

        assertEquals(listOf("active"), listOf("active", "old").filter { repository.get(it).getOrThrow() != null })
    }

    private companion object {
        const val DB_NAME = "alarm-restore-test.db"
    }
}
