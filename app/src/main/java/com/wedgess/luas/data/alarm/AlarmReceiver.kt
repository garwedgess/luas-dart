package com.wedgess.luas.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.wedgess.luas.domain.alarm.AlarmCoordinator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Entry point for everything the OS tells us about alarms: a registration firing, the notification being dismissed,
 * and the system events after which registrations must be rebuilt (reboot, app update, clock, time zone and
 * exact-alarm permission changes). Dismissal is synchronous to block late notification posts; other actions are
 * delegated to [AlarmCoordinator].
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var coordinator: AlarmCoordinator

    @Inject lateinit var notifier: AlarmNotificationNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.data?.takeIf { it.scheme == ALARM_SCHEME }?.lastPathSegment
        if (intent.action == ACTION_DISMISS && id != null && intent.hasExtra(EXTRA_REQUEST_CODE)) {
            notifier.dismiss(id, intent.getIntExtra(EXTRA_REQUEST_CODE, 0))
            return
        }
        val work = alarmWork(intent, coordinator) ?: return

        // The broadcast stays alive while the coordinator works; it bounds its own network wait (a few seconds).
        val pending = goAsync()
        scope.launch {
            runCatching { work() }.onFailure { Timber.e(it, "Alarm broadcast ${intent.action} failed") }
            pending?.finish()
        }
    }

    companion object {
        const val ACTION_ALARM_DUE = "com.wedgess.luas.ACTION_ALARM_DUE"
        const val ACTION_DISMISS = "com.wedgess.luas.ACTION_DISMISS"
        const val ACTION_RESUME = "com.wedgess.luas.ACTION_RESUME"
        internal const val EXTRA_REQUEST_CODE = "reminder_request_code"
        internal const val EXTRA_EXPECTED_AT = "reminder_expected_at"
        internal const val ALARM_SCHEME = "luasalarm"

        /** The permission-change broadcast only exists on Android 12+, so it is referenced by value. */
        private const val ACTION_EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

        internal val RECONCILE_ACTIONS = buildSet {
            add(Intent.ACTION_BOOT_COMPLETED)
            add(Intent.ACTION_MY_PACKAGE_REPLACED)
            add(Intent.ACTION_TIME_CHANGED)
            add(Intent.ACTION_TIMEZONE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(ACTION_EXACT_ALARM_PERMISSION_CHANGED)
        }

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun alarmUri(alarmId: String): Uri = Uri.Builder().scheme(
            ALARM_SCHEME
        ).authority("alarm").appendPath(alarmId).build()
    }
}

/**
 * Maps a broadcast to the coordinator call it stands for, or null for anything unrecognised (including an alarm
 * broadcast without an alarm id). Kept separate from the receiver so the routing can be tested without Hilt.
 */
internal fun alarmWork(intent: Intent, coordinator: AlarmCoordinator): (suspend () -> Unit)? {
    val alarmId = intent.data?.takeIf { it.scheme == AlarmReceiver.ALARM_SCHEME }?.lastPathSegment
    return when (val action = intent.action) {
        AlarmReceiver.ACTION_ALARM_DUE -> alarmId?.let { { coordinator.onAlarmDue(it) } }
        AlarmReceiver.ACTION_DISMISS -> alarmId?.let { { coordinator.dismissNotification(it) } }
        AlarmReceiver.ACTION_RESUME -> alarmId?.let {
            {
                coordinator.resumeNotification(
                    it,
                    java.time.Instant.ofEpochMilli(
                        intent.getLongExtra(AlarmReceiver.EXTRA_EXPECTED_AT, 0)
                    )
                )
            }
        }
        else -> if (action in AlarmReceiver.RECONCILE_ACTIONS) ({ coordinator.reconcile() }) else null
    }
}
