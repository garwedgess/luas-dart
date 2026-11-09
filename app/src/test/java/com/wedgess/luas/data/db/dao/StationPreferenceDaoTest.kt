package com.wedgess.luas.data.db.dao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.db.luasDatabase
import com.wedgess.luas.data.repository.StationPreferencesRepositoryImpl
import com.wedgess.luas.domain.model.StationPreferences
import com.wedgess.luas.domain.model.TransportType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class StationPreferenceDaoTest {

    private lateinit var driver: SqlDriver
    private var now = Instant.parse("2026-11-09T22:00:00Z")
    private val clock = object : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant() = now
    }
    private lateinit var repository: StationPreferencesRepositoryImpl

    @Before
    fun setUp() {
        driver = AndroidSqliteDriver(LuasDatabase.Schema, ApplicationProvider.getApplicationContext<Context>(), null)
        repository = StationPreferencesRepositoryImpl(StationPreferenceDao(luasDatabase(driver)), clock)
    }

    @After
    fun tearDown() = driver.close()

    @Test
    fun `nothing stored gives empty preferences`() = runTest {
        assertEquals(StationPreferences(), repository.observe(TransportType.DART).first())
    }

    @Test
    fun `recents are most recent first and using a station again moves it to the front`() = runTest {
        listOf("A", "B", "C").forEach {
            repository.markUsed(TransportType.LUAS, it)
            now = now.plusSeconds(10)
        }
        repository.markUsed(TransportType.LUAS, "A")

        assertEquals(listOf("A", "C", "B"), repository.observe(TransportType.LUAS).first().recents)
    }

    @Test
    fun `only the five most recent stations are kept as recents`() = runTest {
        ('A'..'H').forEach {
            repository.markUsed(TransportType.LUAS, it.toString())
            now = now.plusSeconds(10)
        }

        assertEquals(listOf("H", "G", "F", "E", "D"), repository.observe(TransportType.LUAS).first().recents)
    }

    @Test
    fun `favouriting is independent of recents and can be undone`() = runTest {
        repository.setFavourite(TransportType.DART, "Howth", true)

        val afterFavourite = repository.observe(TransportType.DART).first()
        assertEquals(setOf("Howth"), afterFavourite.favourites)
        assertEquals("favouriting alone is not a recent use", emptyList<String>(), afterFavourite.recents)

        repository.setFavourite(TransportType.DART, "Howth", false)
        assertEquals(emptySet<String>(), repository.observe(TransportType.DART).first().favourites)
    }

    @Test
    fun `using a favourite keeps it a favourite`() = runTest {
        repository.setFavourite(TransportType.DART, "Howth", true)
        repository.markUsed(TransportType.DART, "Howth")

        assertEquals(setOf("Howth"), repository.observe(TransportType.DART).first().favourites)
    }

    @Test
    fun `Luas and DART preferences never mix`() = runTest {
        repository.setFavourite(TransportType.LUAS, "Same Name", true)
        repository.markUsed(TransportType.DART, "Same Name")

        assertEquals(emptyList<String>(), repository.observe(TransportType.LUAS).first().recents)
        assertEquals(emptySet<String>(), repository.observe(TransportType.DART).first().favourites)
    }
}
