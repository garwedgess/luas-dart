package com.wedgess.luas.data.api.fakes.luas

import com.wedgess.luas.data.loadXML
import com.wedgess.luas.di.NetworkModule.installLuasContentNegotiation
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

object LuasForecastApiServiceFake {

    private val stops = loadXML("luas/forecast.xml")

    fun mockSuccessHttpClient(): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = stops,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "text/html"),
            )
        }
        return HttpClient(mockEngine) {
            installLuasContentNegotiation()
        }
    }

    fun mockErrorHttpClient(): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf("Content-Type", "text/html"),
            )
        }
        return HttpClient(mockEngine) {
            installLuasContentNegotiation()
        }
    }
}
