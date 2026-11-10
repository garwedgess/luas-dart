package com.wedgess.luas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.wedgess.luas.data.alarm.ReminderIntents
import com.wedgess.luas.presentation.main.compose.MainContent
import com.wedgess.luas.presentation.navigation.reminder.ReminderNavigationViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val reminderNavigation: ReminderNavigationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) ReminderIntents.alarmId(intent)?.let(reminderNavigation::open)
        enableEdgeToEdge()
        setContent {
            MainContent(reminderNavigation = reminderNavigation)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ReminderIntents.alarmId(intent)?.let(reminderNavigation::open)
    }
}
