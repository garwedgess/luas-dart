package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.repository.AlarmRepository
import javax.inject.Inject

class RequestExactAlarmPermissionUseCase @Inject constructor(
    private val repository: AlarmRepository
) {

    operator fun invoke() {
        repository.openExactAlarmPermissionSettings()
    }
}
