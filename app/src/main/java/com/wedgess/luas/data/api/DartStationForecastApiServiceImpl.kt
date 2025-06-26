package com.wedgess.luas.data.api

import com.wedgess.luas.data.model.DartApiResult
import com.wedgess.luas.data.model.DartStationForecastResponseData
import com.wedgess.luas.data.utils.extensions.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import javax.inject.Inject

class DartStationForecastApiServiceImpl @Inject constructor(val client: HttpClient) : DartStationForecastApiService {

    override suspend fun fetchForecast(
        stationCode: String,
        numMinutes: Int,
    ): DartApiResult<DartStationForecastResponseData> {
        return client.requestResult {
            url {
                host = "api.irishrail.ie"
                encodedPath = "realtime/realtime.asmx/getStationDataByCodeXML_WithNumMins"
                protocol = URLProtocol.HTTP
                parameters["StationCode"] = stationCode
                parameters["NumMins"] = numMinutes.toString()
            }
            header("Accept", "application/xml")
        }
    }
}
