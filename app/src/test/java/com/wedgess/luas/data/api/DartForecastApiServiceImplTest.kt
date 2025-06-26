package com.wedgess.luas.data.api

import com.wedgess.luas.data.api.fakes.dart.DartForecastApiServiceFake
import com.wedgess.luas.data.api.fakes.luas.LuasForecastApiServiceFake
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DartForecastApiServiceImplTest {

    @Test
    fun `GIVEN fetchForecast is successful THEN success is returned`() = runTest {
        val httpClient = DartForecastApiServiceFake.mockSuccessHttpClient()
        val target: DartStationForecastApiService = DartStationForecastApiServiceImpl(httpClient)

        val result = target.fetchForecast("GSTNS")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `GIVEN fetchForecast fails THEN failure is returned`() = runTest {
        val httpClient = DartForecastApiServiceFake.mockErrorHttpClient()
        val target: DartStationForecastApiService = DartStationForecastApiServiceImpl(httpClient)

        val result = target.fetchForecast("GSTNS")

        assertTrue(result.isFailure)
    }
}
