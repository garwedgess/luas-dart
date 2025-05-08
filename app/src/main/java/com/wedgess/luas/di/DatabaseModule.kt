package com.wedgess.luas.di

import android.content.Context
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.wedgess.luas.Stop
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.db.dao.StopDao
import com.wedgess.luas.data.db.utils.luasLineAdapter
import com.wedgess.luas.data.db.utils.uuidAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "luas.db"

    @Singleton
    @Provides
    fun dbDriver(@ApplicationContext context: Context): AndroidSqliteDriver =
        AndroidSqliteDriver(LuasDatabase.Schema, context, DB_NAME)

    @Singleton
    @Provides
    fun provideDb(driver: AndroidSqliteDriver) = LuasDatabase(
        driver,
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
            },
        ),
    )

    @Singleton
    @Provides
    fun provideStopDao(db: LuasDatabase) = StopDao(db)
}
