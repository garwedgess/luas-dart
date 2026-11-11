package com.wedgess.luas.presentation.forecast.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wedgess.luas.domain.alarm.AlarmCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The app bar lists reminders even while the selected station is loading or offline. */
@HiltViewModel
class ForecastRemindersViewModel @Inject constructor(private val coordinator: AlarmCoordinator) : ViewModel() {
    val alarms = coordinator.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cancel(id: String) {
        viewModelScope.launch { coordinator.cancel(id) }
    }
}
