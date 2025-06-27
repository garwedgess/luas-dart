package com.wedgess.luas.data.api

import com.wedgess.luas.data.model.DartApiResult
import com.wedgess.luas.data.model.DartStationForecastResponseData

interface DartStationForecastApiService {

    suspend fun fetchForecast(stationCode: String, numMinutes: Int = 90): DartApiResult<DartStationForecastResponseData>
}
