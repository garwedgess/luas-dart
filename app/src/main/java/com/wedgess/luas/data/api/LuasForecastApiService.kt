package com.wedgess.luas.data.api

import com.wedgess.luas.data.model.StopForcastResponseData

interface LuasForecastApiService {

    suspend fun fetchForecast(stopAbrv: String): LuasApiResult<StopForcastResponseData>
}