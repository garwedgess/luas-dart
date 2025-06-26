package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.repository.PreferencesRepository
import javax.inject.Inject

class UpdateSelectedDartStationUseCase @Inject constructor(private val preferencesRepository: PreferencesRepository) {

    suspend operator fun invoke(code: String): Result<Unit> = preferencesRepository.updateSelectedDartStation(code)
}
