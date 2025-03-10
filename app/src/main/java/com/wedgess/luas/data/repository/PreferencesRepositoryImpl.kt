package com.wedgess.luas.data.repository

import androidx.datastore.core.DataStore
import com.wedgess.luas.data.model.UserPreferences
import com.wedgess.luas.data.utils.extensions.runWithErrorHandling
import com.wedgess.luas.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    private val preferences: DataStore<UserPreferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PreferencesRepository {

    override suspend fun updateSelectedRedLineStation(abbreviation: String): Result<Unit> {
        return withContext(ioDispatcher) {
            runWithErrorHandling {
                preferences.updateData { preferences ->
                    preferences.toBuilder().setSelectedRedLineStation(abbreviation).build()
                }
            }
        }
    }

    override suspend fun updateSelectedGreenLineStation(abbreviation: String): Result<Unit> {
        return withContext(ioDispatcher) {
            runWithErrorHandling {
                preferences.updateData { preferences ->
                    preferences.toBuilder().setSelectedGreenLineStation(abbreviation).build()
                }
            }
        }
    }

    override fun fetchSelectedGreenLineStation(): Flow<String> {
        return preferences.data.map { it.selectedGreenLineStation }
    }

    override fun fetchSelectedRedLineStation(): Flow<String> {
        return preferences.data.map { it.selectedRedLineStation }
    }
}
