package com.wedgess.luas

import android.app.Application
import android.content.pm.ApplicationInfo
import com.wedgess.luas.domain.alarm.AlarmCoordinator
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class LuasApplication : Application() {

    @Inject
    lateinit var alarmCoordinator: AlarmCoordinator

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        // Release builds plant no tree, so nothing (including network payloads) is logged there.
        if (isDebuggable()) {
            Timber.plant(Timber.DebugTree())
        }
        // Alarm registrations belong to the OS and are lost on some events; the database is authoritative, so rebuild
        // them whenever the process starts (idempotent).
        appScope.launch { alarmCoordinator.reconcile() }
    }

    private fun isDebuggable() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}
