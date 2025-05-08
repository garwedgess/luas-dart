package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WasNotificationPermissionRequestedUseCase @Inject constructor(private val preferencesRepository: PreferencesRepository) {

    operator fun invoke(): Flow<Boolean> = preferencesRepository.wasNotificationPermissionRequested()
}
