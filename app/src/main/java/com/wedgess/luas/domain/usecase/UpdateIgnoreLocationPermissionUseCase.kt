package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.repository.PreferencesRepository
import javax.inject.Inject

class UpdateIgnoreLocationPermissionUseCase @Inject constructor(private val preferencesRepository: PreferencesRepository) {

    suspend operator fun invoke(ignore: Boolean): Result<Unit> =
        preferencesRepository.updateIgnoreLocationPermission(ignore)
}
