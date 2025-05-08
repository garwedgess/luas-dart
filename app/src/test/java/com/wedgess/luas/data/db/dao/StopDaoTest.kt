package com.wedgess.luas.data.db.dao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.wedgess.luas.Stop
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.db.utils.luasLineAdapter
import com.wedgess.luas.data.db.utils.uuidAdapter
import com.wedgess.luas.data.mapper.toDao
import com.wedgess.luas.data.model.LuasLineData
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.StopEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StopDaoTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: LuasDatabase
    private lateinit var dao: StopDao
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        driver = AndroidSqliteDriver(
            LuasDatabase.Schema,
            context,
            null
        )
        database = LuasDatabase(
            driver = driver,
            StopAdapter = Stop.Adapter(
                IdAdapter = uuidAdapter,
                LineAdapter = luasLineAdapter,
                LatitudeAdapter = object : ColumnAdapter<Double, Double> {
                    override fun decode(databaseValue: Double): Double {
                        return databaseValue
                    }

                    override fun encode(value: Double): Double {
                        return value
                    }
                },
                LongitudeAdapter = object : ColumnAdapter<Double, Double> {
                    override fun decode(databaseValue: Double): Double {
                        return databaseValue
                    }

                    override fun encode(value: Double): Double {
                        return value
                    }
                }
            )
        )
        dao = StopDao(database)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `insert - inserts stops correctly`() = runTest {
        val stops = listOf(StopEntity.initial().toDao(), StopEntity.initial().toDao())

        dao.insert(stops)

        val result = dao.fetchAll().first()
        assertEquals(2, result.size)
    }

    @Test
    fun `fetchByLine - fetches correct stops by line`() = runTest {
        val stops = listOf(
            StopEntity.initial().copy(line = LuasLineEntity.GREEN).toDao(),
            StopEntity.initial().copy(line = LuasLineEntity.RED).toDao()
        )

        dao.insert(stops)

        val result = dao.fetchAllByLine(LuasLineData.RED).first()
        assertEquals(1, result.size)
    }
}
