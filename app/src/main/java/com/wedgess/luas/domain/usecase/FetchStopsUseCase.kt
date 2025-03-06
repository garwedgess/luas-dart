package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.domain.repository.LuasRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchStopsUseCase @Inject constructor(val repository: LuasRepository) {

    operator fun invoke(line: LuasLineEntity): Flow<Result<List<StopEntity>>> {
        return repository.fetchStops(line)
    }
}
