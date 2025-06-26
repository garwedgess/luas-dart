package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.PreferencesRepository
import javax.inject.Inject

class UpdateSelectedTransportTypeUseCase @Inject constructor(private val preferencesRepository: PreferencesRepository) {

    suspend operator fun invoke(transportType: TransportType): Result<Unit> {
        return preferencesRepository.updateSelectedTransportType(transportType)
    }
}
