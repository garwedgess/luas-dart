package com.wedgess.luas.data.api

import com.wedgess.luas.data.api.fakes.LuasStopApiServiceFake
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class LuasStopApiServiceImplTest {

    @Test
    fun `GIVEN fetchStops is successful THEN success is returned`() = runTest {
        val httpClient = LuasStopApiServiceFake.mockSuccessHttpClient()
        val target: LuasStopApiService = LuasStopApiServiceImpl(httpClient)

        val result = target.fetchStops()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `GIVEN fetchStops fails THEN failure is returned`() = runTest {
        val httpClient = LuasStopApiServiceFake.mockErrorHttpClient()
        val target: LuasStopApiService = LuasStopApiServiceImpl(httpClient)

        val result = target.fetchStops()

        assertTrue(result.isFailure)
    }
}
