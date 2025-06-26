package com.wedgess.luas.data.api

import com.wedgess.luas.data.model.DartApiResult
import com.wedgess.luas.data.model.DartStationForecastResponseData
import com.wedgess.luas.data.model.LuasApiResult
import com.wedgess.luas.data.model.LuasStopsResponseData

interface DartStationForecastApiService {

    suspend fun fetchForecast(stationCode: String, numMinutes: Int = 90): DartApiResult<DartStationForecastResponseData>
}
