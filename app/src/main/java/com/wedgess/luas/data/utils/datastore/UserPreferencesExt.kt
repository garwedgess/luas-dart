package com.wedgess.luas.data.utils.datastore

import com.wedgess.luas.data.model.UserPreferences

fun UserPreferences.Builder.defaultValues(): UserPreferences.Builder =
    this.setSelectedGreenLineStation("")
        .setSelectedRedLineStation("")
        .setLocationPermissionRequested(false)
        .setNotificationPermissionRequested(false)
        .setIgnoreLocationPermission(false)
