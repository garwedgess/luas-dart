package com.wedgess.luas.di

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.wedgess.luas.DartStation
import com.wedgess.luas.LuasStop
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.db.dao.DartStationDao
import com.wedgess.luas.data.db.dao.LuasStopDao
import com.wedgess.luas.data.db.utils.doubleAdapter
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

    @Singleton
    @Provides
    fun provideStopDao(db: LuasDatabase) = LuasStopDao(db)

    @Singleton
    @Provides
    fun provideDartStationDao(db: LuasDatabase) = DartStationDao(db)
}
