package com.wedgess.luas.domain.repository

import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.LuasStopEntity
import kotlinx.coroutines.flow.Flow

interface LuasRepository {

    fun fetchStops(line: LuasLineEntity): Flow<Result<List<LuasStopEntity>>>

    fun fetchAllStopLocations(): Flow<Result<List<LocationEntity>>>

    fun fetchAllStops(): Flow<Result<List<LuasStopEntity>>>

    suspend fun fetchForecast(stopAbv: String): Result<Departures>

    /**
     * The last stored live result for the station, with its original fetch time, or null when none exists or it is
     * too old to show even as stale. A returned value is never live and must be presented as stale.
     */
    suspend fun cachedForecast(stopAbv: String): Departures?
}
