package com.wedgess.luas.data.repository

import com.wedgess.luas.data.api.LuasForecastApiService
import com.wedgess.luas.data.api.LuasStopApiService
import com.wedgess.luas.data.db.dao.LuasStopDao
import com.wedgess.luas.data.mapper.fromEntity
import com.wedgess.luas.data.mapper.toDao
import com.wedgess.luas.data.mapper.toEntity
import com.wedgess.luas.data.mapper.toLocationEntity
import com.wedgess.luas.data.utils.extensions.resultOf
import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.domain.model.LuasForcastEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.repository.LuasRepository
import kotlinx.coroutines.CancellationException
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
    private val stopsDao: LuasStopDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LuasRepository {

    override fun fetchStops(line: LuasLineEntity): Flow<Result<List<LuasStopEntity>>> {
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

    override fun fetchAllStopLocations(): Flow<Result<List<LocationEntity>>> {
        return stopsDao.fetchAll().map { stops -> stops.map { it.toLocationEntity() } }.resultOf()
    }

    override fun fetchAllStops(): Flow<Result<List<LuasStopEntity>>> {
        return stopsDao.fetchAll()
            .map { localStops ->
                localStops.map {
                    it.toEntity()
                }
            }.resultOf()
    }

    override suspend fun fetchForecast(stopAbv: String): Result<LuasForcastEntity> =
        withContext(ioDispatcher) {
            try {
                forecastApi.fetchForecast(stopAbv).mapCatching { it.toEntity() }
                    .onFailure { Timber.e(it, "Call has failed: ${it.message}") }
            } catch (ce: CancellationException) {
                Timber.e(ce, "Call has failed: ${ce.message}")
                Result.failure(ce)
            }
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
            },
        )
    }
}
