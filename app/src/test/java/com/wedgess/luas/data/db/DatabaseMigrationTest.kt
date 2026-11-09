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
 * Opens the committed version-1 schema snapshot (the schema before persistence was added), runs the real upgrade
 * path through [LuasDatabase.Schema] and checks existing data survives and the new tables work.
 * Gradle's verifySqlDelightMigration separately proves the migrations reproduce the current schema exactly.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val target = context.getDatabasePath(DB_NAME).apply { parentFile?.mkdirs() }
        File(SNAPSHOT_DIR, "1.db").copyTo(target, overwrite = true)
        // Snapshots hold the tables only; an installed version-1 app additionally has user_version = 1, which is
        // what tells the driver to migrate instead of creating the schema again.
        SQLiteDatabase.openDatabase(target.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL("PRAGMA user_version = 1")
        }
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    private fun userVersion(): Long {
        val path = context.getDatabasePath(DB_NAME).path
        return SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("PRAGMA user_version", null).use {
                it.moveToFirst()
                it.getLong(0)
            }
        }
    }

    @Test
    fun `the committed snapshot is the version-1 schema and the current schema is the next version`() {
        assertEquals(1L, userVersion())
        assertEquals(2L, LuasDatabase.Schema.version)
    }

    @Test
    fun `upgrading from version 1 keeps existing data and adds working persistence tables`() = runTest {
        SQLiteDatabase.openDatabase(context.getDatabasePath(DB_NAME).path, null, SQLiteDatabase.OPEN_READWRITE).use {
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
            assertEquals(0, LuasStopDao(db).fetchAll().first().size)
        } finally {
            driver.close()
        }
        assertEquals(2L, userVersion())
    }

    @Test
    fun `a database created fresh is the same version as an upgraded one`() {
        val fresh = "fresh-migration-test.db"
        val driver = AndroidSqliteDriver(LuasDatabase.Schema, context, fresh)
        luasDatabase(driver).alarmQueries.selectByState("ACTIVE").executeAsList() // opens (and creates) the file
        driver.close()
        try {
            val path = context.getDatabasePath(fresh).path
            val version = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("PRAGMA user_version", null).use {
                    it.moveToFirst()
                    it.getLong(0)
                }
            }
            assertEquals(LuasDatabase.Schema.version, version)
        } finally {
            context.deleteDatabase(fresh)
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
        const val SNAPSHOT_DIR = "src/main/sqldelight/databases"
    }
}
