package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.domain.repository.LuasRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchAllStopsUseCase @Inject constructor(
    private val luasRepository: LuasRepository
) {
    operator fun invoke(): Flow<Result<List<StopEntity>>> = luasRepository.fetchAllStops()
}
