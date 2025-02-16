package com.wedgess.luas.di

import com.wedgess.luas.domain.repository.LocationRepository
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.usecase.FetchAllStopsUseCase
import com.wedgess.luas.domain.usecase.FetchCurrentLocationUseCase
import com.wedgess.luas.domain.usecase.FetchForecastUseCase
import com.wedgess.luas.domain.usecase.FetchStopsUseCase
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
    fun provideFetchStopsUseCase(repository: LuasRepository) = FetchStopsUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideFetchForecastUseCase(repository: LuasRepository) = FetchForecastUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideFetchAllStopsUseCase(repository: LuasRepository) = FetchAllStopsUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideFetchCurrentLocationUseCase(repository: LocationRepository) =
        FetchCurrentLocationUseCase(repository)
}
