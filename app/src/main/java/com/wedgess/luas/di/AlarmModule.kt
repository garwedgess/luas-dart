package com.wedgess.luas.di

import com.wedgess.luas.data.alarm.AlarmManagerScheduler
import com.wedgess.luas.data.alarm.AlarmNotificationNotifier
import com.wedgess.luas.domain.alarm.AlarmNotifier
import com.wedgess.luas.domain.alarm.AlarmScheduler
import com.wedgess.luas.domain.navigation.ServiceNavigator
import com.wedgess.luas.presentation.navigation.service.ServiceNavigatorImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmModule {

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(scheduler: AlarmManagerScheduler): AlarmScheduler

    @Binds
    @Singleton
    abstract fun bindAlarmNotifier(notifier: AlarmNotificationNotifier): AlarmNotifier

    companion object {
        @Provides
        fun provideNavigationUseCase(): ServiceNavigator = ServiceNavigatorImpl()
    }
}
