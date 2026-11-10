package com.wedgess.luas.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.db.dao.AlarmDao
import com.wedgess.luas.data.db.dao.DepartureSnapshotDao
import com.wedgess.luas.data.db.dao.LuasStopDao
import com.wedgess.luas.data.db.dao.SelectedStationDao
import com.wedgess.luas.data.db.dao.StationPreferenceDao
import com.wedgess.luas.data.mapper.toAlarm
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.Instant

/**
 * Opens each committed schema snapshot (the schema of an earlier release), runs the real upgrade path through
 * [LuasDatabase.Schema] and checks existing data survives and the new tables work.
 * Gradle's verifySqlDelightMigration separately proves the migrations reproduce the current schema exactly.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    /** Installs the snapshot for [version] as the app's database, the way an app at that version would have it. */
    private fun install(version: Int, populate: (SQLiteDatabase) -> Unit = {}) {
        val target = context.getDatabasePath(DB_NAME).apply { parentFile?.mkdirs() }
        File(SNAPSHOT_DIR, "$version.db").copyTo(target, overwrite = true)
        SQLiteDatabase.openDatabase(target.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            // Snapshots hold the tables only; an installed app additionally has user_version set, which tells the
            // driver to migrate instead of creating the schema again.
            it.execSQL("PRAGMA user_version = $version")
            populate(it)
        }
    }

    private fun userVersion(name: String = DB_NAME): Long =
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("PRAGMA user_version", null).use {
                it.moveToFirst()
                it.getLong(0)
            }
        }

    @Test
    fun `every released schema has a committed snapshot and the current schema is the latest`() {
        (1..5).forEach { assertEquals(true, File(SNAPSHOT_DIR, "$it.db").exists()) }
        assertEquals(5L, LuasDatabase.Schema.version)
    }

    @Test
    fun `upgrading from version 1 keeps existing data and adds working persistence tables including station preferences`() = runTest {
        install(1) {
            it.execSQL(
                "INSERT INTO DartStation(Id, Name, Code, Alias, Latitude, Longitude) " +
                    "VALUES (1, 'Malahide', 'MHIDE', NULL, 53.45, -6.15)"
            )
        }

        val driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
        try {
            val db = luasDatabase(driver)
            assertEquals(1, db.dartStationQueries.selectAll().executeAsList().size)

            val snapshots = DepartureSnapshotDao(db)
            snapshots.save(TransportType.LUAS, "STI", Departures("STI", "Stillorgan", Instant.EPOCH, "", emptyList()))
            assertNotNull(snapshots.load(TransportType.LUAS, "STI"))

            SelectedStationDao(db).save("DART", "Malahide")
            assertEquals("Malahide", SelectedStationDao(db).observe("DART").first())

            assertEquals(emptyList<Any>(), AlarmDao(db).findByState("ACTIVE"))
            StationPreferenceDao(db).setFavourite("DART", "Howth", true)
            assertEquals(1, db.stationPreferenceQueries.selectByTransport("DART").executeAsList().size)
            assertEquals(0, LuasStopDao(db).fetchAll().first().size)
        } finally {
            driver.close()
        }
        assertEquals(5L, userVersion())
    }

    @Test
    fun `upgrading from version 2 keeps stored alarms, gives each its own request code and a reminder time`() = runTest {
        val expected = 1_800_000_000_000L
        install(2) { db ->
            listOf("legacy-a" to "ACTIVE", "legacy-b" to "CANCELLED", "legacy-c" to "FIRED").forEach { (id, state) ->
                db.execSQL(
                    "INSERT INTO Alarm(Id, Kind, State, Transport, StationCode, StationName, Direction, Destination, " +
                        "NotifyMinutesBefore, TripCode, OperatingDate, TargetExpectedAtMillis, TargetObservedAtMillis, " +
                        "CreatedAtMillis, UpdatedAtMillis) VALUES ('$id', 'SPECIFIC_DEPARTURE', '$state', 'LUAS', " +
                        "'STI', 'Stillorgan', 'INBOUND', 'Broombridge', 4, NULL, NULL, $expected, 5, 6, 7)"
                )
            }
        }

        val driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
        try {
            val dao = AlarmDao(luasDatabase(driver))
            val active = dao.findByState("ACTIVE").single().toAlarm()
            assertEquals("legacy-a", active.id)
            assertEquals("Broombridge", active.destination)
            // Reminder = expected - lead (4 min) - the 30 s safety margin the old scheduler used.
            assertEquals(Instant.ofEpochMilli(expected - 4 * 60_000 - 30_000), active.reminderAt)
            assertEquals(0, active.checkCount)
            assertEquals(AlarmState.CANCELLED, dao.findById("legacy-b")!!.toAlarm().state)
            assertEquals(AlarmState.FIRED, dao.findById("legacy-c")!!.toAlarm().state)

            val codes = listOf("legacy-a", "legacy-b", "legacy-c").map { dao.findById(it)!!.RequestCode }
            assertEquals("request codes are unique", 3, codes.toSet().size)

            // New alarms continue after the migrated codes.
            val added = dao.insertIfAbsent(dao.findById("legacy-a")!!.copy(Id = "new"))
            assertEquals(codes.max() + 1, added.RequestCode)
        } finally {
            driver.close()
        }
        assertEquals(5L, userVersion())
    }

    @Test
    fun `upgrading from version 1 through every migration reaches the current schema`() = runTest {
        install(1)

        val driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
        try {
            AlarmDao(luasDatabase(driver)).findByState("ACTIVE")
        } finally {
            driver.close()
        }

        assertEquals(LuasDatabase.Schema.version, userVersion())
    }

    @Test
    fun `upgrading v4 preserves favourites and selections and creates timetable tables`() = runTest {
        install(4) {
            it.execSQL("INSERT INTO StationPreference VALUES ('DART', 'Bray', 1, 100)")
            it.execSQL("INSERT INTO SelectedStation VALUES ('DART', 'Bray')")
        }
        val driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
        try {
            val db = luasDatabase(driver)
            assertEquals("Bray", SelectedStationDao(db).observe("DART").first())
            assertEquals("Bray", StationPreferenceDao(db).observe("DART").first().single().StationName)
            assertEquals(emptyList<Any>(), db.timetableQueries.selectStations("DART").executeAsList())
            db.timetableQueries.insertStation("DART", "bray", "Bray")
            assertEquals("Bray", db.timetableQueries.selectStations("DART").executeAsOne().Name)
        } finally {
            driver.close()
        }
        assertEquals(5L, userVersion())
    }

    @Test
    fun `every earlier snapshot upgrades with working timetable storage`() {
        (1..4).forEach { version ->
            context.deleteDatabase(DB_NAME)
            install(version)
            val driver = AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)
            try {
                val db = luasDatabase(driver)
                db.timetableQueries.insertStation("LUAS", "abbey", "Abbey Street")
                assertEquals("Abbey Street", db.timetableQueries.selectStations("LUAS").executeAsOne().Name)
            } finally {
                driver.close()
            }
            assertEquals(5L, userVersion())
        }
    }

    @Test
    fun `a database created fresh has the same version as an upgraded one`() {
        val fresh = "fresh-migration-test.db"
        val driver = AndroidSqliteDriver(LuasDatabase.Schema, context, fresh)
        luasDatabase(driver).alarmQueries.selectByState("ACTIVE").executeAsList() // opens (and creates) the file
        driver.close()
        try {
            assertEquals(LuasDatabase.Schema.version, userVersion(fresh))
        } finally {
            context.deleteDatabase(fresh)
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
        const val SNAPSHOT_DIR = "src/main/sqldelight/databases"
    }
}
