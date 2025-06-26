package com.wedgess.luas.data.utils.datastore

import com.wedgess.luas.data.model.UserPreferences

fun UserPreferences.Builder.defaultValues(): UserPreferences.Builder =
    this.setSelectedGreenLineStation("")
        .setSelectedRedLineStation("")
        .setSelectedDartStation("")
        .setLocationPermissionRequested(false)
        .setNotificationPermissionRequested(false)
        .setIgnoreLocationPermission(false)
        .setTransportType(UserPreferences.Transport.LUAS)
