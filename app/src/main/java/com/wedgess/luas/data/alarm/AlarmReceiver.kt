package com.wedgess.luas.data.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wedgess.luas.R
import com.wedgess.luas.data.model.NotificationData
import com.wedgess.luas.domain.navigation.ServiceNavigator
import com.wedgess.luas.domain.repository.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var navigationUseCase: ServiceNavigator

    @Inject
    lateinit var alarmRepository: AlarmRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DISMISS) {
            val notificationManager = ContextCompat.getSystemService(
                context,
                NotificationManager::class.java,
            ) ?: return
            notificationManager.cancel(NOTIFICATION_ID)
            alarmRepository.cancelAlarm()
            return
        }

        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        val notificationData = intent.getParcelableExtra<NotificationData>(EXTRA_NOTIFICATION_DATA) ?: return

        Timber.d("Trigger, alarm is triggered, alarmId: $alarmId, notificationData: $notificationData")

        if (alarmId != -1) {
            val notificationManager = ContextCompat.getSystemService(
                context,
                NotificationManager::class.java,
            ) ?: return
            createNotificationChannelIfNeeded(context, notificationManager)
            val notification = buildNotification(
                context,
                notificationData.notificationTitle,
                notificationData.notifyBeforeMins,
                alarmId,
            )
            notificationManager.notify(NOTIFICATION_ID, notification)
            alarmRepository.cancelAlarm()
        }
    }

    private fun createNotificationChannelIfNeeded(context: Context, notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Luas Notifications",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Shows notifications for tram departure"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        context: Context,
        title: String,
        minutesRemaining: Int,
        alarmId: Int,
    ) = NotificationCompat.Builder(context, CHANNEL_ID).apply {
        val dismissIntent = createDismissIntent(context, alarmId)
        setContentTitle(title)
        setContentText(context.resources.getQuantityString(R.plurals.due_minutes, minutesRemaining, minutesRemaining))
        setSmallIcon(R.drawable.ic_tram)
        setPriority(NotificationCompat.PRIORITY_HIGH)
        setCategory(NotificationCompat.CATEGORY_ALARM)
        setOngoing(true)
        setAutoCancel(false)
        setOnlyAlertOnce(true)
        setContentIntent(createContentIntent(context))
        setTimeoutAfter(-1)
        addAction(
            R.drawable.ic_close,
            context.getString(R.string.action_dismiss),
            dismissIntent,
        )
            .setDeleteIntent(dismissIntent)
    }.build()

    private fun createContentIntent(context: Context): PendingIntent {
        val intent = Intent(context, navigationUseCase.mainActivityClass()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createDismissIntent(context: Context, alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }


    companion object {
        const val EXTRA_ALARM_ID = "EXTRA_ALARM_ID"
        const val EXTRA_NOTIFICATION_DATA = "EXTRA_NOTIFICATION_DATA"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "countdown_channel"
        const val ACTION_DISMISS = "com.wedgess.luas.ACTION_DISMISS"
    }
}
