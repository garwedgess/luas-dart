package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.model.StationLocationEntity
import com.wedgess.luas.domain.repository.DartRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchAllDartStationLocationsUseCase @Inject constructor(
    private val dartRepository: DartRepository,
) {
    operator fun invoke(): Flow<Result<List<StationLocationEntity>>> = dartRepository.fetchAllStationLocations()
}
