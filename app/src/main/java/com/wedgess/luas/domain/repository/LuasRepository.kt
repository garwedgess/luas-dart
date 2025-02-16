package com.wedgess.luas.domain.repository

import com.wedgess.luas.domain.model.ForcastEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.StopEntity
import kotlinx.coroutines.flow.Flow

interface LuasRepository {

    fun fetchStops(line: LuasLineEntity): Flow<Result<List<StopEntity>>>

    fun fetchAllStops(): Flow<Result<List<StopEntity>>>

    suspend fun fetchForecast(stopAbv: String): Result<ForcastEntity>
}