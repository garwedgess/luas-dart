package com.wedgess.luas.data.timetable

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.db.luasDatabase
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.timetable.TimetableFeed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TimetableStorageTest {
    @get:Rule val files = TemporaryFolder()
    private lateinit var driver: AndroidSqliteDriver
    private lateinit var dao: TimetableDao
    private val parser = GtfsParser()
    private val source = mockk<GtfsSource>()
    private val clock = mockk<Clock>()
    private val now = Instant.parse("2026-11-10T09:00:00Z")
    private lateinit var repository: TimetableRepositoryImpl
    private lateinit var parsed: GtfsFeed

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        driver = AndroidSqliteDriver(LuasDatabase.Schema, context, null)
        dao = TimetableDao(luasDatabase(driver))
        parsed = parser.parse(fixtureZip(files.newFile()), TransportType.DART)
        every { clock.instant() } returns now
        repository = TimetableRepositoryImpl(dao, source, parser, clock)
    }

    @After fun close() {
        driver.close()
    }

    private fun metadata(data: GtfsFeed = parsed) = TimetableFeed(
        data.transport, now, now, data.schedule.services.dates.min(), data.schedule.services.dates.max(),
        data.zone, data.schedule.times.maxOf { it.seconds }, "\"etag\"", "last-modified", data.version
    )

    @Test
    fun `civil date includes prior service day after midnight and omits non-boarding arrivals`() {
        dao.replace(parsed, metadata())
        val nov10 = LocalDate.of(2026, 11, 10)
        val late = dao.day(TransportType.DART, "dublin connolly", nov10).departures.single()
        assertEquals(Instant.parse("2026-11-10T23:50:00Z"), late.scheduledAt)
        val next = dao.day(TransportType.DART, "bray", nov10.plusDays(1)).departures.single()
        assertEquals(Instant.parse("2026-11-11T01:10:00Z"), next.scheduledAt)
        assertEquals(nov10, next.serviceDate)
        assertTrue(dao.day(TransportType.DART, "bray", nov10).departures.isEmpty())
    }

    @Test
    fun `selected date beyond calendars is not published despite feed metadata year`() {
        dao.replace(parsed, metadata())
        assertFalse(dao.day(TransportType.DART, "bray", LocalDate.of(2027, 1, 1)).published)
        assertEquals(LocalDate.of(2026, 11, 13), dao.feed(TransportType.DART)?.validUntil)
    }

    @Test
    fun `calendar removals override weekly service and Luas uncovered dates are not inferred`() {
        val changed = parsed.copy(
            schedule = parsed.schedule.copy(
                services = parsed.schedule.services.copy(
                    exceptions = parsed.schedule.services.exceptions +
                        GtfsException("weekday", LocalDate.of(2026, 11, 10), false)
                )
            )
        )
        dao.replace(changed, metadata(changed))
        assertTrue(dao.day(TransportType.DART, "dublin connolly", LocalDate.of(2026, 11, 10)).departures.isEmpty())
        val luas = parser.parse(fixtureZip(files.newFile()), TransportType.LUAS)
        dao.replace(luas, metadata(luas))
        assertFalse(dao.day(TransportType.LUAS, "abbey street", LocalDate.of(2026, 12, 25)).published)
    }

    @Test
    fun `times beyond forty-eight hours retain the original service day`() {
        val changed = parsed.copy(
            schedule = parsed.schedule.copy(
                times = parsed.schedule.times.map {
                    if (it.tripId == "t2") it.copy(seconds = 177_300) else it
                }
            )
        )
        dao.replace(changed, metadata(changed))
        val result = dao.day(TransportType.DART, "bray", LocalDate.of(2026, 11, 12)).departures.single()
        assertEquals(Instant.parse("2026-11-12T01:15:00Z"), result.scheduledAt)
        assertEquals(LocalDate.of(2026, 11, 10), result.serviceDate)
    }

    @Test
    fun `failed SQL import rolls back data and metadata together`() {
        dao.replace(parsed, metadata())
        val times = parsed.schedule.times + parsed.schedule.times.first()
        val duplicate = parsed.copy(schedule = parsed.schedule.copy(times = times))
        assertThrows(Exception::class.java) { dao.replace(duplicate, metadata().copy(version = "broken")) }
        assertEquals("fixture", dao.feed(TransportType.DART)?.version)
        assertEquals(2, dao.stations(TransportType.DART).size)
        assertEquals(1, dao.day(TransportType.DART, "bray", LocalDate.of(2026, 11, 11)).departures.size)
    }

    @Test
    fun `cancelled import rolls back and does not affect another transport`() {
        dao.replace(parsed, metadata())
        val luas = parser.parse(fixtureZip(files.newFile()), TransportType.LUAS)
        dao.replace(luas, metadata(luas))
        assertThrows(CancellationException::class.java) {
            dao.replace(parsed, metadata().copy(version = "cancelled")) { throw CancellationException() }
        }
        assertEquals("fixture", dao.feed(TransportType.DART)?.version)
        assertEquals("Abbey Street", dao.stations(TransportType.LUAS).single().name)
        assertEquals(setOf("Green"), dao.stations(TransportType.LUAS).single().lines)
        assertEquals(setOf("DART"), dao.stations(TransportType.DART).first().lines)
    }

    @Test
    fun `304 checks the feed without rewriting download time or timetable`() = runTest {
        dao.replace(parsed, metadata())
        every { clock.instant() } returns now.plusSeconds(90_000)
        coEvery { source.download(TransportType.DART, any()) } returns GtfsDownload.Unchanged
        assertTrue(repository.refresh(TransportType.DART).isSuccess)
        assertEquals(now, repository.feed(TransportType.DART)?.downloadedAt)
        assertEquals(now.plusSeconds(90_000), repository.feed(TransportType.DART)?.checkedAt)
        assertEquals(2, repository.stations(TransportType.DART).size)
    }

    @Test
    fun `automatic checks are throttled but explicit update bypasses throttle`() = runTest {
        dao.replace(parsed, metadata())
        assertTrue(repository.refresh(TransportType.DART).isSuccess)
        coVerify(exactly = 0) { source.download(any(), any()) }
        coEvery { source.download(any(), any()) } returns GtfsDownload.Unchanged
        assertTrue(repository.refresh(TransportType.DART, force = true).isSuccess)
        coVerify(exactly = 1) { source.download(any(), any()) }
    }

    @Test
    fun `failed download and corrupt replacement preserve the existing timetable`() = runTest {
        dao.replace(parsed, metadata())
        coEvery { source.download(any(), any()) } throws IOException("Offline")
        assertTrue(repository.refresh(TransportType.DART, force = true).isFailure)
        val broken = files.newFile().apply { writeText("not a zip") }
        coEvery { source.download(any(), any()) } returns GtfsDownload.Archive(broken, "new", null)
        assertTrue(repository.refresh(TransportType.DART, force = true).isFailure)
        assertFalse(broken.exists())
        assertEquals(metadata(), repository.feed(TransportType.DART))
        assertEquals(2, repository.stations(TransportType.DART).size)
    }

    @Test
    fun `successful update atomically installs parsed data and cleans the archive`() = runTest {
        val archive = fixtureZip(files.newFile())
        coEvery { source.download(TransportType.DART, null) } returns GtfsDownload.Archive(archive, "new", "date")
        assertTrue(repository.refresh(TransportType.DART, force = true).isSuccess)
        assertEquals("new", repository.feed(TransportType.DART)?.etag)
        assertNotNull(repository.day(TransportType.DART, "bray", LocalDate.of(2026, 11, 11)))
        assertFalse(archive.exists())
    }
}
