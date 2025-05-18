package com.wedgess.luas.di

import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.presentation.forecast.tab.viewmodel.ForecastTabViewModel
import dagger.assisted.AssistedFactory

@AssistedFactory
interface ForecastTabViewModelFactory {
    fun create(luasLine: LuasLineEntity): ForecastTabViewModel
}
