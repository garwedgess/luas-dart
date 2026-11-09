package com.wedgess.luas.data.db

import app.cash.sqldelight.db.SqlDriver
import com.wedgess.luas.DartStation
import com.wedgess.luas.LuasStop
import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.db.utils.doubleAdapter
import com.wedgess.luas.data.db.utils.luasLineAdapter
import com.wedgess.luas.data.db.utils.uuidAdapter

fun luasDatabase(driver: SqlDriver) = LuasDatabase(
    driver = driver,
    DartStationAdapter = DartStation.Adapter(
        LatitudeAdapter = doubleAdapter,
        LongitudeAdapter = doubleAdapter
    ),
    LuasStopAdapter = LuasStop.Adapter(
        IdAdapter = uuidAdapter,
        LineAdapter = luasLineAdapter,
        LatitudeAdapter = doubleAdapter,
        LongitudeAdapter = doubleAdapter
    )
)
