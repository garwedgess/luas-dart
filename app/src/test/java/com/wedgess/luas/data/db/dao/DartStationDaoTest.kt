package com.wedgess.luas.data.db.dao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.wedgess.luas.DartStation
import com.wedgess.luas.LuasStop
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.db.utils.doubleAdapter
import com.wedgess.luas.data.db.utils.luasLineAdapter
import com.wedgess.luas.data.db.utils.uuidAdapter
import com.wedgess.luas.data.mapper.toDao
import com.wedgess.luas.domain.model.DartStationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DartStationDaoTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: LuasDatabase
    private lateinit var dao: DartStationDao
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        driver = AndroidSqliteDriver(
            LuasDatabase.Schema,
            context,
            null,
        )
        database = LuasDatabase(
            driver = driver,
            DartStationAdapter = DartStation.Adapter(
                LatitudeAdapter = doubleAdapter,
                LongitudeAdapter = doubleAdapter,
            ),
            LuasStopAdapter = LuasStop.Adapter(
                IdAdapter = uuidAdapter,
                LineAdapter = luasLineAdapter,
                LatitudeAdapter = doubleAdapter,
                LongitudeAdapter = doubleAdapter,
            ),
        )
        dao = DartStationDao(database)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `insert - inserts stops correctly`() = runTest {
        val stops = listOf(
            DartStationEntity.initial(id = 1),
            DartStationEntity.initial(id = 2)
        ).map { it.toDao() }

        dao.insert(stops)

        val result = dao.fetchAll().first()
        assertEquals(2, result.size)
    }
}
