package com.wedgess.luas.domain

import com.wedgess.luas.domain.repository.AlarmRepository
import com.wedgess.luas.domain.usecase.RequestExactAlarmPermissionUseCase
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class RequestExactAlarmPermissionUseCaseTest {

    @MockK
    private lateinit var alarmRepository: AlarmRepository
    private lateinit var requestExactAlarmPermissionUseCase: RequestExactAlarmPermissionUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        requestExactAlarmPermissionUseCase = RequestExactAlarmPermissionUseCase(alarmRepository)
    }

    @Test
    fun `invoke should call openExactAlarmPermissionSettings on repository`() {
        // Given
        // Use justRun for void functions that don't return anything
        justRun { alarmRepository.openExactAlarmPermissionSettings() }

        // When
        requestExactAlarmPermissionUseCase()

        // Then
        verify { alarmRepository.openExactAlarmPermissionSettings() }
    }
}
