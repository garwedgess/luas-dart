package com.wedgess.luas.data.api

import com.wedgess.luas.data.model.DartApiResult
import com.wedgess.luas.data.model.DartStationsResponseData
import com.wedgess.luas.data.utils.extensions.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import javax.inject.Inject

class DartStationsApiServiceImpl @Inject constructor(val client: HttpClient) : DartStationsApiService {

    override suspend fun fetchStations(): DartApiResult<DartStationsResponseData> {
        return client.requestResult {
            url {
                host = "api.irishrail.ie"
                encodedPath = "realtime/realtime.asmx/getAllStationsXML_WithStationType"
                protocol = URLProtocol.HTTPS
                parameters["StationType"] = "D"
            }
            header("Accept", "application/xml")
        }
    }
}
