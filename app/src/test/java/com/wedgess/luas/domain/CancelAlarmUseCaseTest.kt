package com.wedgess.luas.domain

import com.wedgess.luas.domain.repository.AlarmRepository
import com.wedgess.luas.domain.usecase.CancelAlarmUseCase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class CancelAlarmUseCaseTest {

    @MockK
    private lateinit var alarmRepository: AlarmRepository
    private lateinit var cancelAlarmUseCase: CancelAlarmUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        cancelAlarmUseCase = CancelAlarmUseCase(alarmRepository)
    }

    @Test
    fun `invoke should call cancelAlarm on repository`() {
        // Given
        every { alarmRepository.cancelAlarm() } returns Unit

        // When
        cancelAlarmUseCase()

        // Then
        verify { alarmRepository.cancelAlarm() }
    }
}
