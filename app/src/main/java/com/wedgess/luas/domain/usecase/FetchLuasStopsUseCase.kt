package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.repository.LuasRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchLuasStopsUseCase @Inject constructor(val repository: LuasRepository) {

    operator fun invoke(line: LuasLineEntity): Flow<Result<List<LuasStopEntity>>> {
        return repository.fetchStops(line)
    }
}
