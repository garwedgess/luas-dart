package com.wedgess.luas.domain.repository

import com.wedgess.luas.domain.model.TransportType
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    suspend fun updateSelectedRedLineStation(abbreviation: String): Result<Unit>
    suspend fun updateSelectedGreenLineStation(abbreviation: String): Result<Unit>
    suspend fun updateSelectedDartStation(code: String): Result<Unit>
    suspend fun updateLocationPermissionRequested(requested: Boolean): Result<Unit>
    suspend fun updateIgnoreLocationPermission(ignore: Boolean): Result<Unit>
    suspend fun updateNotificationPermissionRequested(requested: Boolean): Result<Unit>
    suspend fun updateIgnoreNotificationPermission(ignore: Boolean): Result<Unit>
    suspend fun updateSelectedTransportType(transportType: TransportType): Result<Unit>
    fun fetchSelectedGreenLineStation(): Flow<String>
    fun fetchSelectedRedLineStation(): Flow<String>
    fun fetchSelectedDartStation(): Flow<String>
    fun fetchSelectedTransportType(): Flow<TransportType>
    fun ignoreLocationPermission(): Flow<Boolean>
    fun ignoreNotificationPermission(): Flow<Boolean>
    fun wasLocationPermissionRequested(): Flow<Boolean>
    fun wasNotificationPermissionRequested(): Flow<Boolean>
}
