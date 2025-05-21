package com.wedgess.luas.domain.usecase

import com.wedgess.luas.domain.RefreshFlow
import com.wedgess.luas.domain.model.UserLocation
import com.wedgess.luas.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchCurrentLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    private val refreshFlow = RefreshFlow()

    operator fun invoke(): Flow<UserLocation?> = refreshFlow.flatMapLatest {
        locationRepository.getCurrentLocation()
    }

    fun refresh() = refreshFlow.refresh()
}
