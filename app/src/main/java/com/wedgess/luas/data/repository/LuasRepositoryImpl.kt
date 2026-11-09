package com.wedgess.luas.data.repository

import com.wedgess.luas.data.api.LuasForecastApiService
import com.wedgess.luas.data.api.LuasStopApiService
import com.wedgess.luas.data.db.dao.DepartureSnapshotDao
import com.wedgess.luas.data.db.dao.LuasStopDao
import com.wedgess.luas.data.db.dao.SnapshotRetention
import com.wedgess.luas.data.mapper.fromEntity
import com.wedgess.luas.data.mapper.toDao
import com.wedgess.luas.data.mapper.toDepartures
import com.wedgess.luas.data.mapper.toEntity
import com.wedgess.luas.data.mapper.toLocationEntity
import com.wedgess.luas.data.utils.extensions.resultOf
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.model.isUsableAsStale
import com.wedgess.luas.domain.repository.LuasRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Clock
import javax.inject.Inject

class LuasRepositoryImpl @Inject constructor(
    private val stopsApi: LuasStopApiService,
    private val forecastApi: LuasForecastApiService,
    private val stopsDao: LuasStopDao,
    private val snapshotDao: DepartureSnapshotDao,
    private val clock: Clock = Clock.systemUTC(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
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

    override suspend fun fetchForecast(stopAbv: String): Result<Departures> =
        withContext(ioDispatcher) {
            forecastApi.fetchForecast(stopAbv)
                .mapCatching { it.toDepartures(fetchedAt = clock.instant()) }
                .onSuccess { storeSnapshot(stopAbv, it) }
                .onFailure { Timber.e(it, "Call has failed: ${it.message}") }
        }

    override suspend fun cachedForecast(stopAbv: String): Departures? = withContext(ioDispatcher) {
        resultOf { snapshotDao.load(TransportType.LUAS, stopAbv.uppercase()) }
            .onFailure { Timber.e(it, "Failed to read cached forecast: ${it.message}") }
            .getOrNull()
            ?.takeIf { it.isUsableAsStale(clock.instant()) }
    }

    // A storage failure must never turn a successful live fetch into a failure.
    private fun storeSnapshot(stopAbv: String, departures: Departures) {
        resultOf {
            snapshotDao.save(TransportType.LUAS, stopAbv.uppercase(), departures)
            snapshotDao.deleteFetchedBefore(clock.instant().minus(SnapshotRetention).toEpochMilli())
        }.onFailure { Timber.e(it, "Failed to store forecast snapshot: ${it.message}") }
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
