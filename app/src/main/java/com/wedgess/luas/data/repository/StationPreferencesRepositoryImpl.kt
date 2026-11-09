package com.wedgess.luas.data.repository

import com.wedgess.luas.data.db.dao.StationPreferenceDao
import com.wedgess.luas.data.db.dao.recentNames
import com.wedgess.luas.data.utils.extensions.resultOf
import com.wedgess.luas.domain.model.StationPreferences
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.StationPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import javax.inject.Inject

private const val MAX_RECENTS = 5

class StationPreferencesRepositoryImpl @Inject constructor(
    private val dao: StationPreferenceDao,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : StationPreferencesRepository {

    override fun observe(transport: TransportType): Flow<StationPreferences> =
        dao.observe(transport.name).map { rows ->
            StationPreferences(
                favourites = rows.filter { it.IsFavourite != 0L }.map { it.StationName }.toSet(),
                recents = rows.recentNames(MAX_RECENTS)
            )
        }

    override suspend fun markUsed(transport: TransportType, stationName: String): Result<Unit> =
        withContext(ioDispatcher) { resultOf { dao.markUsed(transport.name, stationName, clock.millis()) } }

    override suspend fun setFavourite(transport: TransportType, stationName: String, favourite: Boolean) =
        withContext(ioDispatcher) { resultOf { dao.setFavourite(transport.name, stationName, favourite) } }
}
