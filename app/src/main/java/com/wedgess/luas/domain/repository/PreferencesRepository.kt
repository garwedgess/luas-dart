package com.wedgess.luas.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    suspend fun updateSelectedRedLineStation(abbreviation: String): Result<Unit>
    suspend fun updateSelectedGreenLineStation(abbreviation: String): Result<Unit>
    fun fetchSelectedGreenLineStation(): Flow<String>
    fun fetchSelectedRedLineStation(): Flow<String>
}
