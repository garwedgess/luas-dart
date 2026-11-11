package com.wedgess.luas.domain

import kotlinx.coroutines.flow.Flow

/** Reports whether the device has a validated internet connection. */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
