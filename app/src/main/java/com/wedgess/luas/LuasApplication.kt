package com.wedgess.luas

import android.app.Application
import com.mapbox.mapboxsdk.Mapbox
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class LuasApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Mapbox.getInstance(this)
        Timber.plant(Timber.DebugTree())
    }
}
