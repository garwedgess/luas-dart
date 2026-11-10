package com.wedgess.luas.di

import android.content.Context
import com.wedgess.luas.data.timetable.GtfsHttpSource
import com.wedgess.luas.data.timetable.GtfsParser
import com.wedgess.luas.data.timetable.GtfsSource
import com.wedgess.luas.data.timetable.TimetableDao
import com.wedgess.luas.data.timetable.TimetableRepositoryImpl
import com.wedgess.luas.domain.timetable.TimetableRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.io.File
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimetableModule {
    @Provides
    @Singleton
    fun repository(dao: TimetableDao, source: GtfsSource, parser: GtfsParser, clock: Clock): TimetableRepository =
        TimetableRepositoryImpl(dao, source, parser, clock)

    @Provides
    @Singleton
    fun source(client: OkHttpClient, @ApplicationContext context: Context): GtfsSource =
        GtfsHttpSource(client, File(context.cacheDir, "gtfs"))
}
