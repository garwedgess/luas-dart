package com.wedgess.luas.data.db.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.wedgess.luas.LuasStop
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.model.LuasLineData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class LuasStopDao @Inject constructor(
    db: LuasDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val queries = db.luasStopQueries

    fun insert(stop: LuasStop) = with(stop) {
        queries.insert(
            Id = this.Id,
            Name = this.Name,
            Abbreviation = this.Abbreviation,
            IsParkRide = this.IsParkRide,
            IsCycleRide = this.IsCycleRide,
            Latitude = this.Latitude,
            Longitude = this.Longitude,
            Line = this.Line,
        )
    }

    fun insert(stops: List<LuasStop>) = stops.forEach(::insert)

    fun fetchAll() = queries.selectAll().asFlow().mapToList(ioDispatcher)

    fun fetchAllByLine(line: LuasLineData) =
        queries.selectByLine(line).asFlow().mapToList(ioDispatcher)
}
