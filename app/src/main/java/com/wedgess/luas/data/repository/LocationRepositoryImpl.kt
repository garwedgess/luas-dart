package com.wedgess.luas.data.repository

import android.annotation.SuppressLint
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.wedgess.luas.domain.model.UserLocation
import com.wedgess.luas.domain.repository.LocationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.util.concurrent.TimeUnit

class LocationRepositoryImpl(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val looper: Looper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LocationRepository {

    @SuppressLint("MissingPermission")
    override fun getCurrentLocation(): Flow<UserLocation> = callbackFlow {
        try {
            trySend(UserLocation(0.0, 0.0))
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                TimeUnit.MINUTES.toMillis(1),
            )
                .setMinUpdateDistanceMeters(10f)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.run {
                        trySend(
                            UserLocation(
                                this.latitude,
                                this.longitude,
                            ),
                        )
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                looper,
            )

            awaitClose { fusedLocationClient.removeLocationUpdates(locationCallback) }
        } catch (e: Exception) {
            Timber.d(e, "Location permission not granted yet")
            trySend(UserLocation(0.0, 0.0))
        }
    }.catch { t ->
        Timber.e(t, "Failed to get current location")
        emit(UserLocation(0.0, 0.0))
    }.flowOn(ioDispatcher)
}
