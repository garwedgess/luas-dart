package com.wedgess.luas.di

import com.google.android.gms.location.FusedLocationProviderClient
import com.wedgess.luas.data.api.LuasForecastApiService
import com.wedgess.luas.data.api.LuasStopApiService
import com.wedgess.luas.data.db.dao.StopsDao
import com.wedgess.luas.data.repository.LocationRepositoryImpl
import com.wedgess.luas.data.repository.LuasRepositoryImpl
import com.wedgess.luas.domain.repository.LocationRepository
import com.wedgess.luas.domain.repository.LuasRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideLuasRepository(
        stopsApiService: LuasStopApiService,
        forecastApiService: LuasForecastApiService,
        stopsDao: StopsDao
    ): LuasRepository =
        LuasRepositoryImpl(stopsApiService, forecastApiService, stopsDao)

    @Provides
    @Singleton
    fun provideLocationRepository(
        fusedLocationProviderClient: FusedLocationProviderClient
    ): LocationRepository =
        LocationRepositoryImpl(fusedLocationProviderClient)
}
