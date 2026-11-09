package com.wedgess.luas

import android.app.Application
import android.content.pm.ApplicationInfo
import com.mapbox.mapboxsdk.Mapbox
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class LuasApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Mapbox.getInstance(this)
        // Release builds plant no tree, so nothing (including network payloads) is logged there.
        if (isDebuggable()) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun isDebuggable() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}
