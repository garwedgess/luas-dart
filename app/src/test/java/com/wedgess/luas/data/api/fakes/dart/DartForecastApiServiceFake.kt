package com.wedgess.luas.data.api.fakes.dart

import com.wedgess.luas.data.loadXML
import com.wedgess.luas.di.NetworkModule.installDartContentNegotiation
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

object DartForecastApiServiceFake {

    private val stops = loadXML("dart/forecast.xml")

    fun mockSuccessHttpClient(): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = stops,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "text/xml"),
            )
        }
        return HttpClient(mockEngine) {
            installDartContentNegotiation()
        }
    }

    fun mockErrorHttpClient(): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf("Content-Type", "text/xml"),
            )
        }
        return HttpClient(mockEngine) {
            installDartContentNegotiation()
        }
    }
}
