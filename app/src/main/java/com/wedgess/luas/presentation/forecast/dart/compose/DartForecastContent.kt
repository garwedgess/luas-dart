package com.wedgess.luas.presentation.forecast.dart.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.presentation.components.DropdownTextField
import com.wedgess.luas.presentation.components.EmptyContent
import com.wedgess.luas.presentation.components.ErrorContent
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.components.sectionedlist.SectionedList
import com.wedgess.luas.presentation.forecast.compose.RefreshProgressIndicator
import com.wedgess.luas.presentation.forecast.dart.DartForecastTabContract
import com.wedgess.luas.presentation.forecast.dart.viewmodel.DartForecastViewModel
import com.wedgess.luas.presentation.model.Compose
import kotlinx.collections.immutable.toImmutableList

@Composable
fun DartForecastContent(
    dartForecastViewModel: DartForecastViewModel = hiltViewModel(),
    setRefreshAction: (() -> Unit) -> Unit
) {
    val uiResult by dartForecastViewModel.uiResult.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        setRefreshAction { dartForecastViewModel.onEvent(DartForecastTabContract.Event.OnRefresh) }
    }

    uiResult.Compose(
        onLoading = {
            LoadingContent("Loading forecast")
        },
        onError = {
            ErrorContent("Failed to load forecast", subTitle = it)
        },
        onEmpty = {
            EmptyContent(it)
        },
        onSuccess = { uiState ->
            Column {
                RefreshProgressIndicator(refreshProgress = uiState.refreshProgress)
                DartListContent(
                    uiState = uiState,
                    onStopSelect = { stop ->
                        dartForecastViewModel.onEvent(
                            DartForecastTabContract.Event.OnStationSelected(stop)
                        )
                    },
                    onSectionToggle = { sectionId ->
                        dartForecastViewModel.onEvent(
                            DartForecastTabContract.Event.OnToggleSection(sectionId)
                        )
                    }
                )
            }
        }
    )
}

@SuppressLint("ComposeModifierMissing")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DartListContent(
    uiState: DartForecastTabContract.UiState,
    onSectionToggle: (Long) -> Unit,
    onStopSelect: (DartStationEntity) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DropdownTextField(
            modifier = Modifier.fillMaxWidth(),
            label = "Station",
            valueFormatter = { item -> item.name },
            options = uiState.stations.toImmutableList(),
            selectedValue = uiState.selectedStation,
            onValueChange = onStopSelect
        )
        SectionedList(
            modifier = Modifier.fillMaxSize(),
            sectionHeader = { DartForecastHeader() },
            state = uiState.sectionedListState,
            onSectionToggle = onSectionToggle
        ) { rowData ->
            DartForecastItemRow(
                destination = rowData.destination,
                eta = rowData.expectedAt,
                dueIn = rowData.dueIn,
                scheduledTime = rowData.scheduledAt,
                late = rowData.late
            )
        }
    }
}
