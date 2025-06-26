package com.wedgess.luas.data.db.utils

import app.cash.sqldelight.ColumnAdapter
import com.wedgess.luas.data.model.LuasLineData
import java.util.UUID

val uuidAdapter = object : ColumnAdapter<UUID, String> {
    override fun decode(databaseValue: String): UUID = UUID.fromString(databaseValue)

    override fun encode(value: UUID): String = value.toString()
}

val luasLineAdapter = object : ColumnAdapter<LuasLineData, Long> {
    override fun decode(databaseValue: Long): LuasLineData = LuasLineData[databaseValue]

    override fun encode(value: LuasLineData): Long = value.key
}

val doubleAdapter = object : ColumnAdapter<Double, Double> {
    override fun decode(databaseValue: Double): Double = databaseValue

    override fun encode(value: Double): Double = value
}
