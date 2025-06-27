package com.wedgess.luas.data.repository

import com.wedgess.luas.data.api.DartStationForecastApiService
import com.wedgess.luas.data.api.DartStationsApiService
import com.wedgess.luas.data.db.dao.DartStationDao
import com.wedgess.luas.data.mapper.toDao
import com.wedgess.luas.data.mapper.toEntity
import com.wedgess.luas.data.mapper.toLocationEntity
import com.wedgess.luas.data.utils.extensions.resultOf
import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.domain.model.DartStationForecastEntity
import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.domain.repository.DartRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class DartRepositoryImpl @Inject constructor(
    private val stationsApi: DartStationsApiService,
    private val forecastApi: DartStationForecastApiService,
    private val stationsDao: DartStationDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DartRepository {

    override fun fetchStations(): Flow<Result<List<DartStationEntity>>> {
        return stationsDao.fetchAll()
            .onEach { localStops ->
                if (localStops.isEmpty()) {
                    fetchFromRemoteAndStore()
                }
            }
            .map { localStops ->
                localStops.map { it.toEntity() }
            }.resultOf()
    }

    override fun fetchAllStationLocations(): Flow<Result<List<LocationEntity>>> {
        return stationsDao.fetchAll().map { stations -> stations.map { it.toLocationEntity() } }.resultOf()
    }

    override suspend fun fetchForecast(stationCode: String): Result<List<DartStationForecastEntity>> =
        withContext(ioDispatcher) {
            forecastApi.fetchForecast(stationCode).mapCatching { it.toEntity() }
                .onFailure { Timber.e(it, "Call has failed: ${it.message}") }
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
            },
        )
    }
}
