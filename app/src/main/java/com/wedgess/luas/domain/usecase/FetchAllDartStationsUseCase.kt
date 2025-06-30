package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.domain.repository.DartRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchAllDartStationsUseCase @Inject constructor(
    private val dartRepository: DartRepository
) {
    operator fun invoke(): Flow<Result<List<DartStationEntity>>> = dartRepository.fetchStations()
}
