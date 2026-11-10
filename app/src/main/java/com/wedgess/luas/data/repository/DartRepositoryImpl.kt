package com.wedgess.luas.data.repository

import com.wedgess.luas.data.api.DartStationForecastApiService
import com.wedgess.luas.data.api.DartStationsApiService
import com.wedgess.luas.data.db.dao.DartStationDao
import com.wedgess.luas.data.db.dao.DepartureSnapshotDao
import com.wedgess.luas.data.db.dao.SnapshotRetention
import com.wedgess.luas.data.mapper.toDao
import com.wedgess.luas.data.mapper.toDepartures
import com.wedgess.luas.data.mapper.toEntity
import com.wedgess.luas.data.mapper.toLocationEntity
import com.wedgess.luas.data.utils.extensions.populateWhenEmpty
import com.wedgess.luas.data.utils.extensions.resultOf
import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.model.isUsableAsStale
import com.wedgess.luas.domain.repository.DartRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Clock
import javax.inject.Inject

class DartRepositoryImpl @Inject constructor(
    private val stationsApi: DartStationsApiService,
    private val forecastApi: DartStationForecastApiService,
    private val stationsDao: DartStationDao,
    private val snapshotDao: DepartureSnapshotDao,
    private val clock: Clock = Clock.systemUTC(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DartRepository {

    private val catalogueMutex = Mutex()

    override fun fetchStations(): Flow<Result<List<DartStationEntity>>> {
        return stationsDao.fetchAll()
            .populateWhenEmpty { catalogueMutex.withLock { fetchFromRemoteAndStore() } }
            .map { localStops ->
                localStops.map { it.toEntity() }
            }.resultOf()
    }

    override fun fetchAllStationLocations(): Flow<Result<List<LocationEntity>>> {
        return stationsDao.fetchAll()
            .populateWhenEmpty { catalogueMutex.withLock { fetchFromRemoteAndStore() } }
            .map { stations -> stations.map { it.toLocationEntity() } }.resultOf()
    }

    override suspend fun fetchForecast(stationCode: String): Result<Departures> =
        withContext(ioDispatcher) {
            forecastApi.fetchForecast(stationCode).mapCatching { it.toDepartures(fetchedAt = clock.instant()) }
                .onSuccess { storeSnapshot(stationCode, it) }
                .onFailure { Timber.e(it, "Call has failed: ${it.message}") }
        }

    override suspend fun cachedForecast(stopAbv: String): Departures? = withContext(ioDispatcher) {
        resultOf { snapshotDao.load(TransportType.DART, stopAbv.uppercase()) }
            .onFailure { Timber.e(it, "Failed to read cached forecast: ${it.message}") }
            .getOrNull()
            ?.takeIf { it.isUsableAsStale(clock.instant()) }
    }

    // A storage failure must never turn a successful live fetch into a failure.
    private fun storeSnapshot(stationCode: String, departures: Departures) {
        resultOf {
            snapshotDao.save(TransportType.DART, stationCode.uppercase(), departures)
            snapshotDao.deleteFetchedBefore(clock.instant().minus(SnapshotRetention).toEpochMilli())
        }.onFailure { Timber.e(it, "Failed to store forecast snapshot: ${it.message}") }
    }

    private suspend fun fetchFromRemoteAndStore() = withContext(ioDispatcher) {
        val result = stationsApi.fetchStations()
        result.fold(
            onSuccess = { lineResponse ->
                val stops = lineResponse.toDao()
                stationsDao.insert(stops)
            },
            onFailure = { error ->
                Timber.e(error, "Failed to insert remote stops: ${error.message}")
            }
        )
    }
}
