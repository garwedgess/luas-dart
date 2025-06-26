package com.wedgess.luas.data.api

import com.wedgess.luas.data.model.LuasApiResult
import com.wedgess.luas.data.model.LuasStopForcastResponseData

interface LuasForecastApiService {

    suspend fun fetchForecast(stopAbrv: String): LuasApiResult<LuasStopForcastResponseData>
}
