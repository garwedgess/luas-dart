package com.wedgess.luas.data.db.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.wedgess.luas.DartStation
import com.wedgess.luas.data.LuasDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class DartStationDao @Inject constructor(
    db: LuasDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val queries = db.dartStationQueries

    fun insert(stop: DartStation) = with(stop) {
        queries.insert(
            Id = this.Id,
            Name = this.Name,
            Code = this.Code,
            Alias = this.Alias,
            Latitude = this.Latitude,
            Longitude = this.Longitude,
        )
    }

    fun insert(stations: List<DartStation>) = stations.forEach(::insert)

    fun fetchAll() = queries.selectAll().asFlow().mapToList(ioDispatcher)
}
