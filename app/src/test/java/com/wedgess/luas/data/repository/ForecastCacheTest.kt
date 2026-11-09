package com.wedgess.luas.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.api.DartStationForecastApiService
import com.wedgess.luas.data.api.DartStationsApiService
import com.wedgess.luas.data.api.LuasForecastApiService
import com.wedgess.luas.data.api.LuasStopApiService
import com.wedgess.luas.data.db.dao.DartStationDao
import com.wedgess.luas.data.db.dao.DepartureSnapshotDao
import com.wedgess.luas.data.db.dao.LuasStopDao
import com.wedgess.luas.data.db.luasDatabase
import com.wedgess.luas.data.model.DartDirectionData
import com.wedgess.luas.data.model.DartLocationTypeData
import com.wedgess.luas.data.model.DartStationForecastResponseData
import com.wedgess.luas.data.model.LuasDirectionKeyData
import com.wedgess.luas.data.model.LuasStopForcastResponseData
import com.wedgess.luas.domain.model.MaxStaleAge
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Cached forecasts keep their original fetch time and expire by the same rule as stale data. */
@RunWith(RobolectricTestRunner::class)
class ForecastCacheTest {

    private lateinit var context: Context
    private lateinit var driver: SqlDriver
    private val luasApi = mockk<LuasForecastApiService>()
    private val dartApi = mockk<DartStationForecastApiService>()
    private val fetchedAt = Instant.parse("2026-11-09T22:00:00Z")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
    }

    @After
    fun tearDown() {
        driver.close()
        context.deleteDatabase(DB_NAME)
    }

    private class MutableClock(var instant: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = instant
    }

    private fun luasRepository(clock: Clock): LuasRepositoryImpl {
        val db = luasDatabase(driver)
        return LuasRepositoryImpl(
            stopsApi = mockk<LuasStopApiService>(),
            forecastApi = luasApi,
            stopsDao = LuasStopDao(db),
            snapshotDao = DepartureSnapshotDao(db),
            clock = clock
        )
    }

    private fun dartRepository(clock: Clock): DartRepositoryImpl {
        val db = luasDatabase(driver)
        return DartRepositoryImpl(
            stationsApi = mockk<DartStationsApiService>(),
            forecastApi = dartApi,
            stationsDao = DartStationDao(db),
            snapshotDao = DepartureSnapshotDao(db),
            clock = clock
        )
    }

    private val luasResponse = LuasStopForcastResponseData(
        created = "2026-11-09T22:00:00",
        stop = "Stillorgan",
        stopAbv = "STI",
        message = "Green Line services operating normally",
        direction = listOf(
            LuasStopForcastResponseData.DirectionData(
                name = LuasDirectionKeyData.INBOUND,
                tram = listOf(LuasStopForcastResponseData.DirectionData.TramData("2", "Broombridge"))
            )
        )
    )

    @Test
    fun `a live Luas fetch is stored and restored after the repository and database are recreated`() = runTest {
        coEvery { luasApi.fetchForecast("STI") } returns Result.success(luasResponse)
        val live = luasRepository(MutableClock(fetchedAt)).fetchForecast("STI").getOrThrow()
        driver.close()

        driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
        val restored = luasRepository(MutableClock(fetchedAt.plusSeconds(90))).cachedForecast("STI")

        assertEquals("the original fetch time is kept, not the restore time", live, restored)
        assertEquals(fetchedAt, restored!!.fetchedAt)
    }

    @Test
    fun `the cache is keyed by the requested code regardless of case`() = runTest {
        coEvery { luasApi.fetchForecast("sti") } returns Result.success(luasResponse)
        val repository = luasRepository(MutableClock(fetchedAt))

        repository.fetchForecast("sti")

        assertNotNull(repository.cachedForecast("STI"))
        assertNotNull(repository.cachedForecast("sti"))
    }

    @Test
    fun `cached data expires after the maximum stale age`() = runTest {
        coEvery { luasApi.fetchForecast("STI") } returns Result.success(luasResponse)
        luasRepository(MutableClock(fetchedAt)).fetchForecast("STI")

        assertNotNull(luasRepository(MutableClock(fetchedAt.plus(MaxStaleAge))).cachedForecast("STI"))
        assertNull(luasRepository(MutableClock(fetchedAt.plus(MaxStaleAge).plusSeconds(1))).cachedForecast("STI"))
    }

    @Test
    fun `cached data from the future is rejected because the clock moved backwards`() = runTest {
        coEvery { luasApi.fetchForecast("STI") } returns Result.success(luasResponse)
        luasRepository(MutableClock(fetchedAt)).fetchForecast("STI")

        assertNull(luasRepository(MutableClock(fetchedAt.minusSeconds(3_600))).cachedForecast("STI"))
    }

    @Test
    fun `a failed fetch leaves the previous cached result untouched`() = runTest {
        coEvery { luasApi.fetchForecast("STI") } returns Result.success(luasResponse)
        luasRepository(MutableClock(fetchedAt)).fetchForecast("STI")
        coEvery { luasApi.fetchForecast("STI") } returns Result.failure(java.io.IOException("offline"))

        val repository = luasRepository(MutableClock(fetchedAt.plusSeconds(60)))
        assertTrue(repository.fetchForecast("STI").isFailure)

        assertEquals(fetchedAt, repository.cachedForecast("STI")!!.fetchedAt)
    }

    @Test
    fun `a storage failure does not fail a successful live fetch`() = runTest {
        coEvery { luasApi.fetchForecast("STI") } returns Result.success(luasResponse)
        val brokenDao = mockk<DepartureSnapshotDao>()
        every { brokenDao.save(any(), any(), any()) } throws IllegalStateException("disk full")
        val db = luasDatabase(driver)
        val repository = LuasRepositoryImpl(mockk(), luasApi, LuasStopDao(db), brokenDao, MutableClock(fetchedAt))

        assertTrue(repository.fetchForecast("STI").isSuccess)
    }

    @Test
    fun `nothing is returned when a station was never fetched`() = runTest {
        assertNull(luasRepository(MutableClock(fetchedAt)).cachedForecast("STI"))
    }

    @Test
    fun `old snapshots are pruned by retention when a new fetch is stored`() = runTest {
        coEvery { luasApi.fetchForecast(any()) } returns Result.success(luasResponse)
        luasRepository(MutableClock(fetchedAt)).fetchForecast("OLD")

        luasRepository(MutableClock(fetchedAt.plusSeconds(25 * 3_600L))).fetchForecast("NEW")

        val dao = DepartureSnapshotDao(luasDatabase(driver))
        assertNull(dao.load(com.wedgess.luas.domain.model.TransportType.LUAS, "OLD"))
        assertNotNull(dao.load(com.wedgess.luas.domain.model.TransportType.LUAS, "NEW"))
    }

    private val dartResponse = DartStationForecastResponseData(
        listOf(
            DartStationForecastResponseData.DartStationForecastData(
                serverTime = "2026-11-09T22:36:58.573",
                trainCode = "E846 ",
                stationFullName = "Malahide",
                stationCode = "MHIDE",
                queryTime = "22:36:58",
                trainDate = "09 Nov 2026",
                origin = "Bray",
                destination = "Malahide",
                originTime = "21:39",
                destinationTime = "22:57",
                status = "En Route",
                lastLocation = "Arrived Clontarf Road",
                dueIn = 22,
                late = 1,
                expArrival = "22:58",
                expDepart = "00:00",
                schArrival = "22:57",
                schDepart = "00:00",
                direction = DartDirectionData.NORTHBOUND,
                trainType = "DART",
                locationType = DartLocationTypeData.DESTINATION
            )
        )
    )

    @Test
    fun `a DART fetch is restored with its train code, operating date and original fetch time`() = runTest {
        coEvery { dartApi.fetchForecast("MHIDE", any()) } returns Result.success(dartResponse)
        val live = dartRepository(MutableClock(fetchedAt)).fetchForecast("MHIDE").getOrThrow()
        driver.close()

        driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
        val restored = dartRepository(MutableClock(fetchedAt.plusSeconds(120))).cachedForecast("MHIDE")

        assertEquals(live, restored)
        assertEquals("E846", restored!!.items.single().tripCode)
    }

    @Test
    fun `the same code for Luas and DART never shares a cache entry`() = runTest {
        coEvery { luasApi.fetchForecast("ABC") } returns Result.success(luasResponse)
        coEvery { dartApi.fetchForecast("ABC", any()) } returns Result.success(dartResponse)
        luasRepository(MutableClock(fetchedAt)).fetchForecast("ABC")
        dartRepository(MutableClock(fetchedAt)).fetchForecast("ABC")

        assertEquals(
            "Broombridge",
            luasRepository(MutableClock(fetchedAt)).cachedForecast("ABC")!!.items.single().destination
        )
        assertEquals(
            "Malahide",
            dartRepository(MutableClock(fetchedAt)).cachedForecast("ABC")!!.items.single().destination
        )
    }

    private companion object {
        const val DB_NAME = "forecast-cache-test.db"
    }
}
