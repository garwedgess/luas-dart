package com.wedgess.luas.di

import com.wedgess.luas.domain.repository.LocationRepository
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.FetchAllLuasStopsUseCase
import com.wedgess.luas.domain.usecase.FetchCurrentLocationUseCase
import com.wedgess.luas.domain.usecase.FetchLuasStopForecastUseCase
import com.wedgess.luas.domain.usecase.FetchLuasStopsUseCase
import com.wedgess.luas.domain.usecase.FetchSelectedLuasStopUseCase
import com.wedgess.luas.domain.usecase.UpdateSelectedLuasStopUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideFetchStopsUseCase(repository: LuasRepository) = FetchLuasStopsUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideFetchForecastUseCase(repository: LuasRepository) = FetchLuasStopForecastUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideFetchAllStopsUseCase(repository: LuasRepository) = FetchAllLuasStopsUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideFetchCurrentLocationUseCase(repository: LocationRepository) =
        FetchCurrentLocationUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideUpdateSelectedStationUseCase(repository: PreferencesRepository) =
        UpdateSelectedLuasStopUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideFetchSelectedStationUseCase(repository: PreferencesRepository) =
        FetchSelectedLuasStopUseCase(repository)
}
