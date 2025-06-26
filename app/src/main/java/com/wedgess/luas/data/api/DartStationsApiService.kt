package com.wedgess.luas.data.api

import com.wedgess.luas.data.model.DartApiResult
import com.wedgess.luas.data.model.DartStationsResponseData

interface DartStationsApiService {

    suspend fun fetchStations(): DartApiResult<DartStationsResponseData>
}
