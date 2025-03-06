package com.wedgess.luas.data.api.fakes

import com.wedgess.luas.data.loadXML
import com.wedgess.luas.di.NetworkModule.installContentNegotiation
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

object LuasStopApiServiceFake {

    private val stops = loadXML("stops.xml")

    fun mockSuccessHttpClient(): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = stops,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "text/html")
            )
        }
        return HttpClient(mockEngine) {
            installContentNegotiation()
        }
    }

    fun mockErrorHttpClient(): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf("Content-Type", "text/html")
            )
        }
        return HttpClient(mockEngine) {
            installContentNegotiation()
        }
    }
}
