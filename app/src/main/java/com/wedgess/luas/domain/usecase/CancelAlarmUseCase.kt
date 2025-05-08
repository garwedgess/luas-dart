package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.repository.AlarmRepository
import javax.inject.Inject

class CancelAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
) {
    operator fun invoke() = alarmRepository.cancelAlarm()
}
