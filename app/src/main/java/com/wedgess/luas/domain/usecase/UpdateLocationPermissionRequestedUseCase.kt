package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.repository.PreferencesRepository
import javax.inject.Inject

class UpdateLocationPermissionRequestedUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {

    suspend operator fun invoke(requested: Boolean): Result<Unit> =
        preferencesRepository.updateLocationPermissionRequested(requested)
}
