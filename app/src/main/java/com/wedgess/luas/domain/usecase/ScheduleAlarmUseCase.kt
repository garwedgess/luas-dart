package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.model.NotificationEntity
import com.wedgess.luas.domain.repository.AlarmRepository
import javax.inject.Inject

class ScheduleAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
) {
    suspend operator fun invoke(secondsFromNow: Long, notificationEntity: NotificationEntity): Result<Long> {
        require(secondsFromNow > 0) { "Minutes must be greater than 0" }
        return alarmRepository.scheduleAlarm(secondsFromNow, notificationEntity)
    }
}
