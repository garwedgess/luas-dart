package com.wedgess.luas.data.alarm

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.wedgess.luas.domain.Polled
import com.wedgess.luas.domain.alarm.LiveReminderSession
import com.wedgess.luas.domain.alarm.ReminderContent
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmVerification
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** The visible reminder itself hosts the foreground service; no extra status notification is needed. */
@AndroidEntryPoint
class LiveReminderService : Service() {
    @Inject lateinit var notifier: AlarmNotificationNotifier

    @Inject lateinit var liveSession: LiveReminderSession

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessions = linkedMapOf<String, Session>()
    private val forecasts = mutableMapOf<Pair<TransportType, String>, SharedFlow<Polled<Departures>>>()
    private var host: String? = null
    private var latestStartId = 0
    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.getStringExtra(EXTRA_ALARM_ID)?.let { stopTracking(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this,
            dismissReceiver,
            IntentFilter(ACTION_DISMISS),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        scope.launch {
            while (true) {
                delay(VISIBILITY_CHECK_MILLIS)
                val visible = notifier.visibleIds()
                sessions.values.filter { it.snapshot.alarm.id !in visible }
                    .map { it.snapshot.alarm.id }.forEach { stopTracking(it) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        val id = intent?.getStringExtra(EXTRA_ALARM_ID)
        val snapshot = id?.let(notifier::snapshot)
        if (snapshot == null) {
            if (sessions.isEmpty()) stopSelf(startId)
            return START_NOT_STICKY
        }
        if (host == null && !promote(snapshot)) {
            notifier.finishTracking(
                snapshot.alarm.id,
                (snapshot.content as ReminderContent.Upcoming).copy(
                    verification = AlarmVerification.UNVERIFIED,
                    backgroundRestricted = true
                )
            )
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (id !in sessions) startTracking(snapshot)
        return START_NOT_STICKY
    }

    @Suppress("TooGenericExceptionCaught") // A failed backend must finish the foreground session cleanly.
    private fun startTracking(snapshot: VisibleReminder) {
        val content = snapshot.content as? ReminderContent.Upcoming ?: return
        val id = snapshot.alarm.id
        val session = Session(snapshot)
        sessions[id] = session
        session.job = scope.launch {
            try {
                val forecast = forecasts.getOrPut(snapshot.alarm.forecastKey()) {
                    liveSession.forecasts(snapshot.alarm).shareIn(
                        scope,
                        SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
                        replay = 1
                    )
                }
                liveSession.updates(snapshot.alarm, content, forecast).collect { updated ->
                    if (updated !is ReminderContent.Upcoming) {
                        stopTracking(id, updated)
                    } else if (!notifier.update(id, updated)) {
                        stopTracking(id)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Live reminder tracking failed")
                val current = notifier.snapshot(id)?.content as? ReminderContent.Upcoming ?: content
                stopTracking(id, current.copy(verification = AlarmVerification.UNVERIFIED))
            }
        }
    }

    private fun promote(snapshot: VisibleReminder): Boolean = try {
        notifier.promote(snapshot.alarm.id) { code, notification ->
            ServiceCompat.startForeground(
                this,
                code,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
            )
            host = snapshot.alarm.id
        }
    } catch (e: IllegalStateException) {
        Timber.w(e, "Android did not allow foreground reminder tracking")
        false
    } catch (e: SecurityException) {
        Timber.w(e, "Android denied foreground reminder tracking")
        false
    }

    private fun stopTracking(id: String, content: ReminderContent? = null) {
        val removed = sessions.remove(id) ?: return
        removed.job?.cancel()
        if (host == id) {
            val next = sessions.keys.firstNotNullOfOrNull(notifier::snapshot)
            if (next == null || !promote(next)) {
                stopAllTracking(removeNotification = content == null)
            }
        }
        if (content == null) notifier.remove(removed.snapshot.alarm) else notifier.finishTracking(id, content)
        val key = removed.snapshot.alarm.forecastKey()
        if (sessions.values.none { it.snapshot.alarm.forecastKey() == key }) forecasts.remove(key)
        if (sessions.isEmpty()) stopSelf(latestStartId)
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopAllTracking()
        stopSelf()
    }

    private fun stopAllTracking(removeNotification: Boolean = false) {
        if (host != null) stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        host = null
        val active = sessions.values.toList()
        sessions.clear()
        forecasts.clear()
        active.forEach {
            it.job?.cancel()
            notifier.finishTracking(it.snapshot.alarm.id)
        }
    }

    override fun onDestroy() {
        stopAllTracking()
        scope.cancel()
        unregisterReceiver(dismissReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private data class Session(val snapshot: VisibleReminder, var job: Job? = null)

    companion object {
        internal const val EXTRA_ALARM_ID = "live_reminder_alarm_id"
        internal const val ACTION_DISMISS = "com.wedgess.luas.DISMISS_LIVE_TRACKING"
        private const val VISIBILITY_CHECK_MILLIS = 5_000L
    }
}

private fun Alarm.forecastKey() = transport to stationCode.trim().uppercase(java.util.Locale.ROOT)
