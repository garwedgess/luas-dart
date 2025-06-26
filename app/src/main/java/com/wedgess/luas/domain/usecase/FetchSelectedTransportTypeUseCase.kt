package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchSelectedTransportTypeUseCase @Inject constructor(private val preferencesRepository: PreferencesRepository) {

    operator fun invoke(): Flow<TransportType> = preferencesRepository.fetchSelectedTransportType()
}
