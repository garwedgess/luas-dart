package com.wedgess.luas.di

import com.wedgess.luas.domain.repository.LocationRepository
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.usecase.FetchAllLuasStopsUseCase
import com.wedgess.luas.domain.usecase.FetchCurrentLocationUseCase
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
    fun provideFetchAllStopsUseCase(repository: LuasRepository) = FetchAllLuasStopsUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideFetchCurrentLocationUseCase(repository: LocationRepository) =
        FetchCurrentLocationUseCase(repository)
}
