package com.wedgess.luas.domain.repository

import com.wedgess.luas.domain.model.StationPreferences
import com.wedgess.luas.domain.model.TransportType
import kotlinx.coroutines.flow.Flow

/** Favourites and recents per transport, so Luas and DART never share or overwrite each other's. */
interface StationPreferencesRepository {
    fun observe(transport: TransportType): Flow<StationPreferences>

    suspend fun markUsed(transport: TransportType, stationName: String): Result<Unit>

    suspend fun setFavourite(transport: TransportType, stationName: String, favourite: Boolean): Result<Unit>
}
