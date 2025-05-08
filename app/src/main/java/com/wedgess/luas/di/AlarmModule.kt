package com.wedgess.luas.di

import android.content.Context
import com.wedgess.luas.data.alarm.AlarmManagerDataSource
import com.wedgess.luas.data.repository.AlarmRepositoryImpl
import com.wedgess.luas.domain.navigation.ServiceNavigator
import com.wedgess.luas.domain.repository.AlarmRepository
import com.wedgess.luas.domain.usecase.CancelAlarmUseCase
import com.wedgess.luas.domain.usecase.ScheduleAlarmUseCase
import com.wedgess.luas.presentation.navigation.service.ServiceNavigatorImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AlarmModule {

    @Provides
    @Singleton
    fun provideAlarmRepository(
        alarmManagerDataSource: AlarmManagerDataSource,
    ): AlarmRepository {
        return AlarmRepositoryImpl(alarmManagerDataSource)
    }

    @Provides
    @Singleton
    fun provideAlarmManagerDataSource(
        @ApplicationContext context: Context,
    ): AlarmManagerDataSource {
        return AlarmManagerDataSource(context)
    }

    @Provides
    fun provideScheduleAlarmUseCase(alarmRepository: AlarmRepository): ScheduleAlarmUseCase {
        return ScheduleAlarmUseCase(alarmRepository)
    }

    @Provides
    fun provideCancelAlarmUseCase(alarmRepository: AlarmRepository): CancelAlarmUseCase {
        return CancelAlarmUseCase(alarmRepository)
    }

    @Provides
    fun provideNavigationUseCase(): ServiceNavigator {
        return ServiceNavigatorImpl()
    }
}
