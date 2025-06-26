package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchSelectedDartStationUseCase @Inject constructor(private val preferencesRepository: PreferencesRepository) {

    operator fun invoke(): Flow<String> = preferencesRepository.fetchSelectedDartStation()
}
