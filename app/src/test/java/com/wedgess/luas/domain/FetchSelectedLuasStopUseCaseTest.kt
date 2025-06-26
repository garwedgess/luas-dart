package com.wedgess.luas.domain

import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.usecase.FetchSelectedLuasStopUseCase
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

class FetchSelectedLuasStopUseCaseTest {

    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var fetchSelectedLuasStopUseCase: FetchSelectedLuasStopUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        fetchSelectedLuasStopUseCase = FetchSelectedLuasStopUseCase(preferencesRepository)
    }

    @Test
    fun `invoke should return red line station when RED line is requested`() = runTest {
        // Given
        val redLineStation = "TAL"
        every { preferencesRepository.fetchSelectedRedLineStation() } returns flowOf(redLineStation)

        // When
        val result = fetchSelectedLuasStopUseCase(LuasLineEntity.RED).first()

        // Then
        verify { preferencesRepository.fetchSelectedRedLineStation() }
        assertEquals(redLineStation, result)
    }

    @Test
    fun `invoke should return green line station when GREEN line is requested`() = runTest {
        // Given
        val greenLineStation = "STI"
        every { preferencesRepository.fetchSelectedGreenLineStation() } returns flowOf(greenLineStation)

        // When
        val result = fetchSelectedLuasStopUseCase(LuasLineEntity.GREEN).first()

        // Then
        verify { preferencesRepository.fetchSelectedGreenLineStation() }
        assertEquals(greenLineStation, result)
    }
}
