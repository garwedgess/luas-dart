package com.wedgess.luas.domain.repository

import com.wedgess.luas.domain.model.LuasForcastEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.model.StationLocationEntity
import kotlinx.coroutines.flow.Flow

interface LuasRepository {

    fun fetchStops(line: LuasLineEntity): Flow<Result<List<LuasStopEntity>>>

    fun fetchAllStopLocations(): Flow<Result<List<StationLocationEntity>>>

    fun fetchAllStops(): Flow<Result<List<LuasStopEntity>>>

    suspend fun fetchForecast(stopAbv: String): Result<LuasForcastEntity>
}
