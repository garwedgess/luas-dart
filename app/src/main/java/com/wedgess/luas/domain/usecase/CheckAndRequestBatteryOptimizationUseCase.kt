package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.repository.AlarmRepository
import javax.inject.Inject

class CheckAndRequestBatteryOptimizationUseCase @Inject constructor(
    private val repository: AlarmRepository,
) {

    operator fun invoke() {
        if (!repository.isBatteryOptimizationIgnored()) {
            repository.openBatteryOptimizationSettings()
        }
    }
}
