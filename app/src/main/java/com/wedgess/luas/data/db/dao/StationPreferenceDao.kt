package com.wedgess.luas.data.db.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.wedgess.luas.StationPreference
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.domain.model.StationNames
import com.wedgess.luas.domain.model.TransportType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class StationPreferenceDao @Inject constructor(
    private val db: LuasDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val queries = db.stationPreferenceQueries

    fun observe(transport: String) = queries.selectByTransport(transport).asFlow().mapToList(ioDispatcher)

    fun markUsed(transport: String, stationName: String, usedAtMillis: Long) = db.transaction {
        queries.insertIfAbsent(transport, stationName)
        queries.touch(usedAtMillis, transport, stationName)
    }

    fun setFavourite(transport: String, stationName: String, favourite: Boolean) = db.transaction {
        val mode = TransportType.valueOf(transport)
        val key = StationNames.matchKey(mode, stationName)
        val aliases = queries.selectByTransport(transport).executeAsList()
            .filter { StationNames.matchKey(mode, it.StationName) == key }.map { it.StationName }
        // Live and timetable feeds can spell the same station differently. Unstarring either clears both names.
        (aliases + stationName).distinct().forEach { name ->
            queries.insertIfAbsent(transport, name)
            queries.updateFavourite(if (favourite) 1L else 0L, transport, name)
        }
    }
}

internal fun List<StationPreference>.recentNames(limit: Int) =
    filter { it.LastUsedAtMillis > 0 }.sortedByDescending { it.LastUsedAtMillis }.take(limit).map { it.StationName }
