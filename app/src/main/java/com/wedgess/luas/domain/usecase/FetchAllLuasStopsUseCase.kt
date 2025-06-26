package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.repository.LuasRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchAllLuasStopsUseCase @Inject constructor(
    private val luasRepository: LuasRepository
) {
    operator fun invoke(): Flow<Result<List<LuasStopEntity>>> = luasRepository.fetchAllStops()
}
