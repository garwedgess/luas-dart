package com.wedgess.luas.data.api

import com.wedgess.luas.data.api.fakes.luas.LuasForecastApiServiceFake
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class LuasForecastApiServiceImplTest {

    @Test
    fun `GIVEN fetchForecast is successful THEN success is returned`() = runTest {
        val httpClient = LuasForecastApiServiceFake.mockSuccessHttpClient()
        val target: LuasForecastApiService = LuasForecastApiServiceImpl(httpClient)

        val result = target.fetchForecast("STS")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `GIVEN fetchForecast fails THEN failure is returned`() = runTest {
        val httpClient = LuasForecastApiServiceFake.mockErrorHttpClient()
        val target: LuasForecastApiService = LuasForecastApiServiceImpl(httpClient)

        val result = target.fetchForecast("STS")

        assertTrue(result.isFailure)
    }
}
