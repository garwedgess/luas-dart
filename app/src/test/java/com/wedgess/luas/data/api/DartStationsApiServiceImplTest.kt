package com.wedgess.luas.data.api

import com.wedgess.luas.data.api.fakes.dart.DartStationsApiServiceFake
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DartStationsApiServiceImplTest {

    @Test
    fun `GIVEN fetchStations is successful THEN success is returned`() = runTest {
        val httpClient = DartStationsApiServiceFake.mockSuccessHttpClient()
        val target: DartStationsApiService = DartStationsApiServiceImpl(httpClient)

        val result = target.fetchStations()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `GIVEN fetchStations fails THEN failure is returned`() = runTest {
        val httpClient = DartStationsApiServiceFake.mockErrorHttpClient()
        val target: DartStationsApiService = DartStationsApiServiceImpl(httpClient)

        val result = target.fetchStations()

        assertTrue(result.isFailure)
    }
}
