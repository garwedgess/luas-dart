package com.wedgess.luas.domain

import com.wedgess.luas.domain.model.UserLocation
import com.wedgess.luas.domain.repository.LocationRepository
import com.wedgess.luas.domain.usecase.FetchCurrentLocationUseCase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FetchCurrentLocationUseCaseTest {

    @MockK
    private lateinit var locationRepository: LocationRepository
    private lateinit var fetchCurrentLocationUseCase: FetchCurrentLocationUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        fetchCurrentLocationUseCase = FetchCurrentLocationUseCase(locationRepository)
    }

    @Test
    fun `invoke should return location from repository`() = runTest {
        // Given
        val mockLocation = UserLocation(53.349805, -6.26031)
        every { locationRepository.getCurrentLocation() } returns flowOf(mockLocation)

        // When
        val result = fetchCurrentLocationUseCase().first()

        // Then
        verify { locationRepository.getCurrentLocation() }
        assertEquals(mockLocation, result)
    }
}
