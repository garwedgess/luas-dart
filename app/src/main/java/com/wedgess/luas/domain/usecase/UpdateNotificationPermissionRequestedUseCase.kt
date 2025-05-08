package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.repository.PreferencesRepository
import javax.inject.Inject

class UpdateNotificationPermissionRequestedUseCase @Inject constructor(private val preferencesRepository: PreferencesRepository) {

    suspend operator fun invoke(granted: Boolean): Result<Unit> =
        preferencesRepository.updateNotificationPermissionRequested(granted)
}
