package com.wedgess.luas.data.repository

import com.wedgess.luas.data.api.LuasForecastApiService
import com.wedgess.luas.data.api.LuasStopApiService
import com.wedgess.luas.data.db.dao.StopsDao
import com.wedgess.luas.data.mapper.fromEntity
import com.wedgess.luas.data.mapper.toDao
import com.wedgess.luas.data.mapper.toEntity
import com.wedgess.luas.data.utils.resultOf
import com.wedgess.luas.domain.model.ForcastEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.domain.repository.LuasRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class LuasRepositoryImpl @Inject constructor(
    private val stopsApi: LuasStopApiService,
    private val forecastApi: LuasForecastApiService,
    private val stopsDao: StopsDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LuasRepository {

    override fun fetchStops(line: LuasLineEntity): Flow<Result<List<StopEntity>>> {
        return stopsDao.fetchAllByLine(line.fromEntity())
            .onEach { localStops ->
                if (localStops.isEmpty()) {
                    fetchFromRemoteAndStore()
                }
            }
            .map { localStops ->
                localStops.map { it.toEntity() }
            }.resultOf()
    }

    override fun fetchAllStops(): Flow<Result<List<StopEntity>>> {
        return stopsDao.fetchAll()
            .map { localStops ->
                localStops.map {
                    it.toEntity()
                }
            }.resultOf()
    }

    override suspend fun fetchForecast(stopAbv: String): Result<ForcastEntity> =
        withContext(ioDispatcher) {
            forecastApi.fetchForecast(stopAbv).mapCatching { it.toEntity() }
        }

    private suspend fun fetchFromRemoteAndStore() = withContext(ioDispatcher) {
        val result = stopsApi.fetchStops()
        result.fold(
            onSuccess = { lineResponse ->
                val stops = lineResponse.toDao()
                stopsDao.insert(stops)
            },
            onFailure = { error ->
                Timber.e(error, "Failed to insert remote stops: ${error.message}")
            }
        )
    }
}
