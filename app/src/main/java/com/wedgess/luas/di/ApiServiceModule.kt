package com.wedgess.luas.di

import com.wedgess.luas.data.api.LuasForecastApiService
import com.wedgess.luas.data.api.LuasForecastApiServiceImpl
import com.wedgess.luas.data.api.LuasStopApiService
import com.wedgess.luas.data.api.LuasStopApiServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiServiceModule {

    @Provides
    @Singleton
    fun provideLuasStopApiService(client: HttpClient): LuasStopApiService =
        LuasStopApiServiceImpl(client)

    @Provides
    @Singleton
    fun provideLuasForecastApiService(client: HttpClient): LuasForecastApiService =
        LuasForecastApiServiceImpl(client)
}
