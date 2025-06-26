package com.wedgess.luas.data.mapper

import com.wedgess.luas.data.model.UserPreferences
import com.wedgess.luas.domain.model.TransportType

fun UserPreferences.Transport.toEntity() = when (this) {
    UserPreferences.Transport.LUAS,
    UserPreferences.Transport.UNRECOGNIZED -> TransportType.LUAS
    UserPreferences.Transport.DART -> TransportType.DART
}

fun TransportType.toData() = when (this) {
    TransportType.LUAS -> UserPreferences.Transport.LUAS
    TransportType.DART -> UserPreferences.Transport.DART
}
