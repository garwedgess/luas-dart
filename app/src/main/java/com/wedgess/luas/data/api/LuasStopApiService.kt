package com.wedgess.luas.data.api

import com.wedgess.luas.data.model.StopsResponseData

interface LuasStopApiService {

    suspend fun fetchStops(): LuasApiResult<StopsResponseData>
}