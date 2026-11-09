package com.wedgess.luas.data.repository

import androidx.datastore.core.DataStore
import com.wedgess.luas.data.db.dao.SelectedStationDao
import com.wedgess.luas.data.mapper.toData
import com.wedgess.luas.data.mapper.toEntity
import com.wedgess.luas.data.model.UserPreferences
import com.wedgess.luas.data.utils.extensions.runWithErrorHandling
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    private val preferences: DataStore<UserPreferences>,
    private val selectedStations: SelectedStationDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PreferencesRepository {

    override suspend fun updateSelectedRedLineStation(abbreviation: String): Result<Unit> {
        return withContext(ioDispatcher) {
            runWithErrorHandling { selectedStations.save(SLOT_LUAS_RED, abbreviation) }
        }
    }

    override suspend fun updateSelectedGreenLineStation(abbreviation: String): Result<Unit> {
        return withContext(ioDispatcher) {
            runWithErrorHandling { selectedStations.save(SLOT_LUAS_GREEN, abbreviation) }
        }
    }

    override suspend fun updateSelectedDartStation(code: String): Result<Unit> {
        return withContext(ioDispatcher) {
            runWithErrorHandling { selectedStations.save(SLOT_DART, code) }
        }
    }

    override suspend fun updateLocationPermissionRequested(requested: Boolean): Result<Unit> {
        return withContext(ioDispatcher) {
            runWithErrorHandling {
                preferences.updateData { preferences ->
                    preferences.toBuilder().setLocationPermissionRequested(requested).build()
                }
            }
        }
    }

    override suspend fun updateIgnoreLocationPermission(ignore: Boolean): Result<Unit> {
        return withContext(ioDispatcher) {
            runWithErrorHandling {
                preferences.updateData { preferences ->
                    preferences.toBuilder().setIgnoreLocationPermission(ignore).build()
                }
            }
        }
    }

    override suspend fun updateNotificationPermissionRequested(requested: Boolean): Result<Unit> {
        return withContext(ioDispatcher) {
            runWithErrorHandling {
                preferences.updateData { preferences ->
                    preferences.toBuilder().setNotificationPermissionRequested(requested).build()
                }
            }
        }
    }

    override suspend fun updateIgnoreNotificationPermission(ignore: Boolean): Result<Unit> {
        return withContext(ioDispatcher) {
            runWithErrorHandling {
                preferences.updateData { preferences ->
                    preferences.toBuilder().setIgnoreNotificationPermission(ignore).build()
                }
            }
        }
    }

    override suspend fun updateSelectedTransportType(transportType: TransportType): Result<Unit> {
        return withContext(ioDispatcher) {
            runWithErrorHandling {
                preferences.updateData { preferences ->
                    preferences.toBuilder().setTransportType(transportType.toData()).build()
                }
            }
        }
    }

    override fun fetchSelectedGreenLineStation(): Flow<String> {
        return selection(SLOT_LUAS_GREEN) { it.selectedGreenLineStation }
    }

    override fun fetchSelectedRedLineStation(): Flow<String> {
        return selection(SLOT_LUAS_RED) { it.selectedRedLineStation }
    }

    override fun fetchSelectedDartStation(): Flow<String> {
        return selection(SLOT_DART) { it.selectedDartStation }
    }

    override fun ignoreLocationPermission(): Flow<Boolean> {
        return preferences.data.map { it.ignoreLocationPermission }
    }

    override fun ignoreNotificationPermission(): Flow<Boolean> {
        return preferences.data.map { it.ignoreNotificationPermission }
    }

    override fun wasLocationPermissionRequested(): Flow<Boolean> {
        return preferences.data.map { it.locationPermissionRequested }
    }

    override fun wasNotificationPermissionRequested(): Flow<Boolean> {
        return preferences.data.map { it.notificationPermissionRequested }
    }

    override fun fetchSelectedTransportType(): Flow<TransportType> {
        return preferences.data.map { it.transportType.toEntity() }
    }

    /**
     * Selections live in SQLDelight, one row per mode/line, so changing one never touches another. A selection saved
     * before that move is still in DataStore and is used until the user picks again.
     */
    private fun selection(slot: String, legacy: (UserPreferences) -> String): Flow<String> =
        combine(selectedStations.observe(slot), preferences.data) { stored, prefs -> stored ?: legacy(prefs) }

    private companion object {
        const val SLOT_LUAS_RED = "LUAS_RED"
        const val SLOT_LUAS_GREEN = "LUAS_GREEN"
        const val SLOT_DART = "DART"
    }
}
