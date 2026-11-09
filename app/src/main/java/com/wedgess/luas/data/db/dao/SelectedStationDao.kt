package com.wedgess.luas.data.db.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.wedgess.luas.data.LuasDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class SelectedStationDao @Inject constructor(
    db: LuasDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val queries = db.selectedStationQueries

    fun observe(slot: String) = queries.selectBySlot(slot).asFlow().mapToOneOrNull(ioDispatcher)

    fun save(slot: String, stationName: String) = queries.upsert(slot, stationName)
}
