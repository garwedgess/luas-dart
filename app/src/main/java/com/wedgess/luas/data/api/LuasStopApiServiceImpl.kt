package com.wedgess.luas.data.api

import com.wedgess.luas.data.model.LuasApiResult
import com.wedgess.luas.data.model.LuasStopsResponseData
import com.wedgess.luas.data.utils.extensions.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import javax.inject.Inject

class LuasStopApiServiceImpl @Inject constructor(val client: HttpClient) : LuasStopApiService {

    override suspend fun fetchStops(): LuasApiResult<LuasStopsResponseData> {
        return client.requestResult {
            url {
                host = "luasforecasts.rpa.ie"
                encodedPath = "xml/get.ashx"
                protocol = URLProtocol.HTTP
                parameters["action"] = "stops"
                parameters["encrypt"] = false.toString()
            }
            header("Accept", "application/xml")
        }
    }
}
