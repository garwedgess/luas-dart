package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class IsNotificationPermissionIgnoredUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {

    suspend operator fun invoke(): Boolean = preferencesRepository.ignoreNotificationPermission().first()
}
