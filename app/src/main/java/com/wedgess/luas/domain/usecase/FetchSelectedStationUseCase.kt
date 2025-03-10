package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchSelectedStationUseCase @Inject constructor(private val preferencesRepository: PreferencesRepository) {

    operator fun invoke(line: LuasLineEntity): Flow<String> {
        return when (line) {
            LuasLineEntity.RED -> preferencesRepository.fetchSelectedRedLineStation()
            LuasLineEntity.GREEN -> preferencesRepository.fetchSelectedGreenLineStation()
        }
    }
}
