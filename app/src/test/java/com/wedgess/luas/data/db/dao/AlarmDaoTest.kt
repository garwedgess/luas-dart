package com.wedgess.luas.data.db.dao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.db.luasDatabase
import com.wedgess.luas.data.mapper.toAlarm
import com.wedgess.luas.data.mapper.toRow
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.TransportType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class AlarmDaoTest {

    private lateinit var driver: SqlDriver
    private lateinit var dao: AlarmDao
    private val created = Instant.parse("2026-11-09T22:00:00Z")
    private val active = "ACTIVE"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        driver = AndroidSqliteDriver(LuasDatabase.Schema, context, null)
        dao = AlarmDao(luasDatabase(driver))
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private fun alarm(id: String = "alarm-1") = Alarm(
        id = id,
        requestCode = 0,
        kind = AlarmKind.SPECIFIC_DEPARTURE,
        state = AlarmState.ACTIVE,
        transport = TransportType.DART,
        stationCode = "MHIDE",
        stationName = "Malahide",
        direction = DepartureDirection.NORTHBOUND,
        destination = "Malahide",
        notifyMinutesBefore = 5,
        tripCode = "E846",
        operatingDate = LocalDate.of(2026, 11, 9),
        targetExpectedAt = created.plusSeconds(1_320),
        targetObservedAt = created,
        reminderAt = created.plusSeconds(990),
        createdAt = created,
        updatedAt = created
    )

    @Test
    fun `an alarm round-trips every field and receives a request code`() {
        val stored = dao.insertIfAbsent(alarm().toRow()).toAlarm()

        assertEquals(alarm().copy(requestCode = 1), stored)
        assertEquals(stored, dao.findById("alarm-1")!!.toAlarm())
    }

    @Test
    fun `request codes are distinct and alarm identity is independent of the departure`() {
        val a = dao.insertIfAbsent(alarm("a").toRow())
        val b = dao.insertIfAbsent(alarm("b").toRow()) // same station, direction and train

        assertEquals(setOf(1L, 2L), setOf(a.RequestCode, b.RequestCode))
        assertEquals(2, dao.findByState(active).size)
    }

    @Test
    fun `inserting an existing id returns the stored alarm and never overwrites it`() {
        dao.insertIfAbsent(alarm().toRow())
        dao.finish("alarm-1", "CANCELLED", "USER_CANCELLED", null, null, 5)

        val again = dao.insertIfAbsent(alarm().copy(notifyMinutesBefore = 99).toRow()).toAlarm()

        assertEquals(AlarmState.CANCELLED, again.state)
        assertEquals(5, again.notifyMinutesBefore)
        assertEquals(1, dao.findByState("CANCELLED").size)
    }

    @Test
    fun `finishing is a conditional transition won by exactly one caller`() {
        dao.insertIfAbsent(alarm().toRow())

        assertTrue(dao.finish("alarm-1", "FIRED", "NOTIFIED", "LIVE_VERIFIED", 10, 10))
        assertFalse(dao.finish("alarm-1", "FIRED", "NOTIFIED", "LIVE_VERIFIED", 11, 11))
        assertFalse(
            "a finished alarm cannot be cancelled afterwards",
            dao.finish("alarm-1", "CANCELLED", "USER_CANCELLED", null, null, 12)
        )

        val row = dao.findById("alarm-1")!!.toAlarm()
        assertEquals(AlarmState.FIRED, row.state)
        assertEquals(Instant.ofEpochMilli(10), row.notifiedAt)
        assertEquals("LIVE_VERIFIED", row.verification!!.name)
    }

    @Test
    fun `updates apply only to active alarms`() {
        dao.insertIfAbsent(alarm().toRow())
        assertTrue(dao.updateSchedule("alarm-1", 100, 200, 50, 2, 60))
        assertTrue(dao.updateRegistration("alarm-1", "INEXACT", 61))
        dao.finish("alarm-1", "CANCELLED", "USER_CANCELLED", null, null, 70)

        assertFalse(dao.updateSchedule("alarm-1", 999, 999, 999, 3, 80))
        assertFalse(dao.updateRegistration("alarm-1", "EXACT", 81))

        val row = dao.findById("alarm-1")!!.toAlarm()
        assertEquals(Instant.ofEpochMilli(100), row.reminderAt)
        assertEquals(2, row.checkCount)
        assertEquals("INEXACT", row.registration!!.name)
    }

    @Test
    fun `updating an unknown alarm changes nothing`() {
        assertFalse(dao.updateSchedule("missing", 1, 1, 1, 1, 1))
        assertFalse(dao.finish("missing", "FIRED", "NOTIFIED", null, null, 1))
        assertNull(dao.findById("missing"))
    }

    @Test
    fun `active alarms are listed by reminder time`() {
        dao.insertIfAbsent(alarm("late").copy(reminderAt = created.plusSeconds(500)).toRow())
        dao.insertIfAbsent(alarm("early").copy(reminderAt = created.plusSeconds(100)).toRow())

        assertEquals(listOf("early", "late"), dao.findByState(active).map { it.Id })
    }

    @Test
    fun `purging removes only finished alarms not updated since the cut-off`() {
        val old = Instant.ofEpochMilli(1)
        dao.insertIfAbsent(alarm("active-old").copy(updatedAt = old).toRow())
        dao.insertIfAbsent(alarm("cancelled-old").toRow())
        dao.finish("cancelled-old", "CANCELLED", "USER_CANCELLED", null, null, 1)
        dao.insertIfAbsent(alarm("fired-new").toRow())
        dao.finish("fired-new", "FIRED", "NOTIFIED", null, null, 100)

        dao.deleteFinishedBefore(50)

        assertNotNull(dao.findById("active-old"))
        assertNull(dao.findById("cancelled-old"))
        assertNotNull(dao.findById("fired-new"))
    }
}
