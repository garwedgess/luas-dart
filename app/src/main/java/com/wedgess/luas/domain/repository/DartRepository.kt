package com.wedgess.luas.domain.repository

import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.LocationEntity
import kotlinx.coroutines.flow.Flow

interface DartRepository {

    fun fetchStations(): Flow<Result<List<DartStationEntity>>>

    fun fetchAllStationLocations(): Flow<Result<List<LocationEntity>>>

    suspend fun fetchForecast(stopAbv: String): Result<Departures>

    /**
     * The last stored live result for the station, with its original fetch time, or null when none exists or it is
     * too old to show even as stale. A returned value is never live and must be presented as stale.
     */
    suspend fun cachedForecast(stopAbv: String): Departures?
}
