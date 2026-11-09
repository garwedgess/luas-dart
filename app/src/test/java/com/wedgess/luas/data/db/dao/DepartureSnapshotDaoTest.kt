package com.wedgess.luas.data.db.dao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.db.luasDatabase
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class DepartureSnapshotDaoTest {

    private lateinit var driver: SqlDriver
    private lateinit var dao: DepartureSnapshotDao
    private val fetchedAt = Instant.parse("2026-11-09T22:00:00.123Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        driver = AndroidSqliteDriver(LuasDatabase.Schema, context, null)
        dao = DepartureSnapshotDao(luasDatabase(driver))
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private val dartTrain = Departure(
        transport = TransportType.DART,
        direction = DepartureDirection.NORTHBOUND,
        destination = "Malahide",
        dueMins = 22,
        expectedAt = fetchedAt.plusSeconds(22 * 60L),
        scheduledAt = fetchedAt.plusSeconds(21 * 60L),
        lateMins = 1,
        tripCode = "E846",
        operatingDate = LocalDate.of(2026, 11, 9),
        status = "En Route",
        lastLocation = "Arrived Clontarf Road"
    )

    private val luasTram = Departure(
        transport = TransportType.LUAS,
        direction = DepartureDirection.INBOUND,
        destination = "Broombridge",
        dueMins = 2,
        expectedAt = fetchedAt.plusSeconds(120)
    )

    private val serviceMessage = luasTram.copy(
        destination = "Services operating with delays",
        dueMins = null,
        expectedAt = null
    )

    private fun departures(code: String, vararg items: Departure, at: Instant = fetchedAt) = Departures(
        stationCode = code,
        stationName = "Station $code",
        fetchedAt = at,
        message = "Operating normally",
        items = items.toList()
    )

    @Test
    fun `a stored result round-trips every field including its original fetch time`() {
        val original = departures("MHIDE", dartTrain)

        dao.save(TransportType.DART, "MHIDE", original)

        assertEquals(original, dao.load(TransportType.DART, "MHIDE"))
    }

    @Test
    fun `optional fields and service messages round-trip as null`() {
        val original = departures("STI", luasTram, serviceMessage)

        dao.save(TransportType.LUAS, "STI", original)

        val loaded = dao.load(TransportType.LUAS, "STI")!!
        assertEquals(original, loaded)
        assertNull(loaded.items[1].dueMins)
        assertNull(loaded.items[1].expectedAt)
        assertNull(loaded.items[0].tripCode)
    }

    @Test
    fun `item order is preserved`() {
        val items = (1..5).map { luasTram.copy(destination = "D$it", dueMins = it) }

        dao.save(TransportType.LUAS, "STI", departures("STI", *items.toTypedArray()))

        assertEquals(items.map { it.destination }, dao.load(TransportType.LUAS, "STI")!!.items.map { it.destination })
    }

    @Test
    fun `saving again replaces the whole result rather than merging two fetches`() {
        dao.save(TransportType.LUAS, "STI", departures("STI", luasTram, luasTram.copy(destination = "Old")))
        val later = fetchedAt.plusSeconds(15)

        dao.save(TransportType.LUAS, "STI", departures("STI", luasTram.copy(destination = "New"), at = later))

        val loaded = dao.load(TransportType.LUAS, "STI")!!
        assertEquals(listOf("New"), loaded.items.map { it.destination })
        assertEquals(later, loaded.fetchedAt)
    }

    @Test
    fun `stations and transports are stored independently even with the same code`() {
        dao.save(TransportType.LUAS, "ABC", departures("ABC", luasTram))
        dao.save(TransportType.DART, "ABC", departures("ABC", dartTrain))
        dao.save(TransportType.LUAS, "XYZ", departures("XYZ", luasTram.copy(destination = "Other")))

        assertEquals("Broombridge", dao.load(TransportType.LUAS, "ABC")!!.items.single().destination)
        assertEquals("Malahide", dao.load(TransportType.DART, "ABC")!!.items.single().destination)
        assertEquals("Other", dao.load(TransportType.LUAS, "XYZ")!!.items.single().destination)
    }

    @Test
    fun `nothing is returned for a station that was never stored`() {
        assertNull(dao.load(TransportType.LUAS, "NONE"))
    }

    @Test
    fun `retention removes old snapshots with their items and keeps newer ones`() {
        val old = fetchedAt.minusSeconds(25 * 3_600L)
        dao.save(TransportType.LUAS, "OLD", departures("OLD", luasTram, at = old))
        dao.save(TransportType.LUAS, "NEW", departures("NEW", luasTram))

        dao.deleteFetchedBefore(fetchedAt.minusSeconds(24 * 3_600L).toEpochMilli())

        assertNull(dao.load(TransportType.LUAS, "OLD"))
        assertEquals(1, dao.load(TransportType.LUAS, "NEW")!!.items.size)
    }

    @Test
    fun `retention does not leave orphaned items behind`() {
        dao.save(TransportType.LUAS, "OLD", departures("OLD", luasTram, at = fetchedAt.minusSeconds(100_000)))

        dao.deleteFetchedBefore(fetchedAt.toEpochMilli())
        // Saving a different, empty result for the same key must not resurrect the deleted items.
        dao.save(TransportType.LUAS, "OLD", departures("OLD"))

        assertEquals(0, dao.load(TransportType.LUAS, "OLD")!!.items.size)
    }
}
