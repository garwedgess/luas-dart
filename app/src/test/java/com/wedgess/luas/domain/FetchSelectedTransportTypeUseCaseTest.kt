package com.wedgess.luas.domain

import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.FetchSelectedTransportTypeUseCase
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

class FetchSelectedTransportTypeUseCaseTest {

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var fetchSelectedTransportTypeUseCase: FetchSelectedTransportTypeUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        fetchSelectedTransportTypeUseCase = FetchSelectedTransportTypeUseCase(preferencesRepository)
    }

    @Test
    fun `invoke should return correct transport type when invoked`() = runTest {
        // Given
        val transportType = TransportType.DART
        every { preferencesRepository.fetchSelectedTransportType() } returns flowOf(transportType)

        // When
        val result = fetchSelectedTransportTypeUseCase().first()

        // Then
        verify { preferencesRepository.fetchSelectedTransportType() }
        assertEquals(transportType, result)
    }
}
