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
import org.junit.Assert.assertNotEquals
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

    private val dartAlarm = Alarm(
        id = Alarm.departureId(TransportType.DART, "MHIDE", "E846", LocalDate.of(2026, 11, 9)),
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
        createdAt = created,
        updatedAt = created
    )

    @Test
    fun `an alarm round-trips every field`() {
        dao.save(dartAlarm.toRow())

        assertEquals(dartAlarm, dao.findById(dartAlarm.id)!!.toAlarm())
    }

    @Test
    fun `saving the same id again updates the one row and keeps its creation time`() {
        dao.save(dartAlarm.toRow())

        dao.save(
            dartAlarm.copy(
                notifyMinutesBefore = 10,
                createdAt = created.plusSeconds(99),
                updatedAt = created.plusSeconds(99)
            ).toRow()
        )

        val rows = dao.findByState("ACTIVE")
        assertEquals(1, rows.size)
        assertEquals(10, rows.single().toAlarm().notifyMinutesBefore)
        assertEquals(created, rows.single().toAlarm().createdAt)
        assertEquals(created.plusSeconds(99), rows.single().toAlarm().updatedAt)
    }

    @Test
    fun `state changes are idempotent`() {
        dao.save(dartAlarm.toRow())

        assertTrue(dao.updateState(dartAlarm.id, "ACTIVE", "CANCELLED", 1))
        assertFalse("a second cancel changes nothing", dao.updateState(dartAlarm.id, "ACTIVE", "CANCELLED", 2))
        assertEquals(AlarmState.CANCELLED, dao.findById(dartAlarm.id)!!.toAlarm().state)
        assertEquals(1L, dao.findById(dartAlarm.id)!!.UpdatedAtMillis)
    }

    @Test
    fun `a finished alarm cannot be moved to a different finished state`() {
        dao.save(dartAlarm.toRow())
        dao.updateState(dartAlarm.id, "ACTIVE", "FIRED", 1)

        assertFalse(dao.updateState(dartAlarm.id, "ACTIVE", "CANCELLED", 2))
        assertEquals(AlarmState.FIRED, dao.findById(dartAlarm.id)!!.toAlarm().state)
    }

    @Test
    fun `updating an unknown id changes nothing`() {
        assertFalse(dao.updateState("missing", "ACTIVE", "CANCELLED", 1))
        assertNull(dao.findById("missing"))
    }

    @Test
    fun `updateStateOfAll only touches active alarms`() {
        val other = dartAlarm.copy(id = "other", state = AlarmState.FIRED)
        dao.save(dartAlarm.toRow())
        dao.save(other.toRow())

        dao.updateStateOfAll("ACTIVE", "CANCELLED", 5)

        assertEquals(AlarmState.CANCELLED, dao.findById(dartAlarm.id)!!.toAlarm().state)
        assertEquals(AlarmState.FIRED, dao.findById("other")!!.toAlarm().state)
    }

    @Test
    fun `purging removes only finished alarms not updated since the cut-off`() {
        dao.save(dartAlarm.copy(id = "active-old", updatedAt = Instant.ofEpochMilli(1)).toRow())
        dao.save(
            dartAlarm.copy(
                id = "cancelled-old",
                state = AlarmState.CANCELLED,
                updatedAt = Instant.ofEpochMilli(1)
            ).toRow()
        )
        dao.save(
            dartAlarm.copy(id = "fired-new", state = AlarmState.FIRED, updatedAt = Instant.ofEpochMilli(100)).toRow()
        )

        dao.deleteFinishedBefore("ACTIVE", 50)

        assertNotEquals(null, dao.findById("active-old"))
        assertNull(dao.findById("cancelled-old"))
        assertNotEquals(null, dao.findById("fired-new"))
    }
}
