package com.wedgess.luas.domain.repository

import com.wedgess.luas.domain.model.UserLocation
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getCurrentLocation(): Flow<UserLocation>
}
