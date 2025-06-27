package com.wedgess.luas.domain.repository

import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.domain.model.DartStationForecastEntity
import com.wedgess.luas.domain.model.LocationEntity
import kotlinx.coroutines.flow.Flow

interface DartRepository {

    fun fetchStations(): Flow<Result<List<DartStationEntity>>>

    fun fetchAllStationLocations(): Flow<Result<List<LocationEntity>>>

    suspend fun fetchForecast(stopAbv: String): Result<List<DartStationForecastEntity>>
}
