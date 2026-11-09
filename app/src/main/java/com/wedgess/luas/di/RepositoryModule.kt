package com.wedgess.luas.di

import android.os.Looper
import androidx.datastore.core.DataStore
import com.google.android.gms.location.FusedLocationProviderClient
import com.wedgess.luas.data.api.DartStationForecastApiService
import com.wedgess.luas.data.api.DartStationsApiService
import com.wedgess.luas.data.api.LuasForecastApiService
import com.wedgess.luas.data.api.LuasStopApiService
import com.wedgess.luas.data.db.dao.AlarmDao
import com.wedgess.luas.data.db.dao.DartStationDao
import com.wedgess.luas.data.db.dao.DepartureSnapshotDao
import com.wedgess.luas.data.db.dao.LuasStopDao
import com.wedgess.luas.data.db.dao.SelectedStationDao
import com.wedgess.luas.data.model.UserPreferences
import com.wedgess.luas.data.repository.AlarmConfigRepositoryImpl
import com.wedgess.luas.data.repository.DartRepositoryImpl
import com.wedgess.luas.data.repository.LocationRepositoryImpl
import com.wedgess.luas.data.repository.LuasRepositoryImpl
import com.wedgess.luas.data.repository.PreferencesRepositoryImpl
import com.wedgess.luas.domain.repository.AlarmConfigRepository
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.repository.LocationRepository
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.repository.PreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideLuasRepository(
        stopsApiService: LuasStopApiService,
        forecastApiService: LuasForecastApiService,
        stopsDao: LuasStopDao,
        snapshotDao: DepartureSnapshotDao,
        clock: Clock
    ): LuasRepository =
        LuasRepositoryImpl(stopsApiService, forecastApiService, stopsDao, snapshotDao, clock)

    @Provides
    @Singleton
    fun provideDartRepository(
        stopsApiService: DartStationsApiService,
        forecastApiService: DartStationForecastApiService,
        stopsDao: DartStationDao,
        snapshotDao: DepartureSnapshotDao,
        clock: Clock
    ): DartRepository =
        DartRepositoryImpl(stopsApiService, forecastApiService, stopsDao, snapshotDao, clock)

    @Provides
    @Singleton
    fun provideLocationRepository(
        fusedLocationProviderClient: FusedLocationProviderClient
    ): LocationRepository =
        LocationRepositoryImpl(fusedLocationProviderClient, Looper.getMainLooper())

    @Provides
    @Singleton
    fun providePreferencesRepository(
        dataStore: DataStore<UserPreferences>,
        selectedStationDao: SelectedStationDao
    ): PreferencesRepository =
        PreferencesRepositoryImpl(dataStore, selectedStationDao)

    @Provides
    @Singleton
    fun provideAlarmConfigRepository(alarmDao: AlarmDao, clock: Clock): AlarmConfigRepository =
        AlarmConfigRepositoryImpl(alarmDao, clock)
}
