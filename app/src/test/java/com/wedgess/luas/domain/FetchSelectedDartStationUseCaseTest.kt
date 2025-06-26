package com.wedgess.luas.domain

import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.FetchSelectedDartStationUseCase
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

class FetchSelectedDartStationUseCaseTest {

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var fetchSelectedDartStationUseCase: FetchSelectedDartStationUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        fetchSelectedDartStationUseCase = FetchSelectedDartStationUseCase(preferencesRepository)
    }

    @Test
    fun `invoke should return correct station when invoked`() = runTest {
        // Given
        val station = "GSTNS"
        every { preferencesRepository.fetchSelectedDartStation() } returns flowOf(station)

        // When
        val result = fetchSelectedDartStationUseCase().first()

        // Then
        verify { preferencesRepository.fetchSelectedDartStation() }
        assertEquals(station, result)
    }
}
