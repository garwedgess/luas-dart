package com.wedgess.luas.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    suspend fun updateSelectedRedLineStation(abbreviation: String): Result<Unit>
    suspend fun updateSelectedGreenLineStation(abbreviation: String): Result<Unit>
    suspend fun updateLocationPermissionRequested(requested: Boolean): Result<Unit>
    suspend fun updateIgnoreLocationPermission(ignore: Boolean): Result<Unit>
    suspend fun updateNotificationPermissionRequested(requested: Boolean): Result<Unit>
    suspend fun updateIgnoreNotificationPermission(ignore: Boolean): Result<Unit>
    fun fetchSelectedGreenLineStation(): Flow<String>
    fun fetchSelectedRedLineStation(): Flow<String>
    fun ignoreLocationPermission(): Flow<Boolean>
    fun ignoreNotificationPermission(): Flow<Boolean>
    fun wasLocationPermissionRequested(): Flow<Boolean>
    fun wasNotificationPermissionRequested(): Flow<Boolean>
}
