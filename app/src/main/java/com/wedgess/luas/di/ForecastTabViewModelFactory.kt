package com.wedgess.luas.di

import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.presentation.forecast.viewmodel.ForecastViewModel
import dagger.assisted.AssistedFactory

@AssistedFactory
interface ForecastTabViewModelFactory {
    fun create(luasLine: LuasLineEntity): ForecastViewModel
}
