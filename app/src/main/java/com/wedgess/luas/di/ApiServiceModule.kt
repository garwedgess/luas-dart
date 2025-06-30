package com.wedgess.luas.di

import com.wedgess.luas.data.api.DartStationForecastApiService
import com.wedgess.luas.data.api.DartStationForecastApiServiceImpl
import com.wedgess.luas.data.api.DartStationsApiService
import com.wedgess.luas.data.api.DartStationsApiServiceImpl
import com.wedgess.luas.data.api.LuasForecastApiService
import com.wedgess.luas.data.api.LuasForecastApiServiceImpl
import com.wedgess.luas.data.api.LuasStopApiService
import com.wedgess.luas.data.api.LuasStopApiServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiServiceModule {

    @Provides
    @Singleton
    fun provideLuasStopApiService(
        @Named("LuasHttpClient") client: HttpClient
    ): LuasStopApiService =
        LuasStopApiServiceImpl(client)

    @Provides
    @Singleton
    fun provideLuasForecastApiService(
        @Named("LuasHttpClient") client: HttpClient
    ): LuasForecastApiService =
        LuasForecastApiServiceImpl(client)

    @Provides
    @Singleton
    fun provideDartStationApiService(
        @Named("DartHttpClient") client: HttpClient
    ): DartStationsApiService =
        DartStationsApiServiceImpl(client)

    @Provides
    @Singleton
    fun provideDartStationForecastApiService(
        @Named("DartHttpClient") client: HttpClient
    ): DartStationForecastApiService =
        DartStationForecastApiServiceImpl(client)
}
