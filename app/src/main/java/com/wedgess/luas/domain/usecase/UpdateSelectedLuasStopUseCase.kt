package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.repository.PreferencesRepository
import javax.inject.Inject

class UpdateSelectedLuasStopUseCase @Inject constructor(private val preferencesRepository: PreferencesRepository) {

    suspend operator fun invoke(abbreviation: String, line: LuasLineEntity): Result<Unit> {
        return when (line) {
            LuasLineEntity.RED -> preferencesRepository.updateSelectedRedLineStation(abbreviation)
            LuasLineEntity.GREEN -> preferencesRepository.updateSelectedGreenLineStation(abbreviation)
        }
    }
}
