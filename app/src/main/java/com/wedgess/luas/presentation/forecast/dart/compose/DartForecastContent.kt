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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wedgess.luas.R
import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.presentation.components.DropdownTextField
import com.wedgess.luas.presentation.components.EmptyContent
import com.wedgess.luas.presentation.components.ErrorContent
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.components.sectionedlist.SectionedList
import com.wedgess.luas.presentation.forecast.dart.DartForecastTabContract
import com.wedgess.luas.presentation.forecast.dart.viewmodel.DartForecastViewModel
import com.wedgess.luas.presentation.model.Compose
import kotlinx.collections.immutable.toImmutableList

@Composable
fun DartForecastContent(
    onRefreshAction: (() -> Unit) -> Unit,
    onProgressChange: (Float) -> Unit,
    dartForecastViewModel: DartForecastViewModel = hiltViewModel(),
) {
    val uiResult by dartForecastViewModel.uiResult.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        onRefreshAction { dartForecastViewModel.onEvent(DartForecastTabContract.Event.OnRefresh) }
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
            DartListContent(
                uiState = uiState,
                onStopSelect = { stop ->
                    dartForecastViewModel.onEvent(
                        DartForecastTabContract.Event.OnStationSelected(stop),
                    )
                },
                onProgressChange = onProgressChange,
                onSectionToggle = { sectionId ->
                    dartForecastViewModel.onEvent(
                        DartForecastTabContract.Event.OnToggleSection(sectionId),
                    )
                },
            )
        },
    )
}

@SuppressLint("ComposeModifierMissing")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DartListContent(
    uiState: DartForecastTabContract.UiState,
    onSectionToggle: (Long) -> Unit,
    onStopSelect: (DartStationEntity) -> Unit,
    onProgressChange: (Float) -> Unit,
) {
    LaunchedEffect(uiState.refreshProgress) {
        onProgressChange(uiState.refreshProgress)
    }
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
            onValueChange = onStopSelect,
        )
        SectionedList(
            modifier = Modifier.fillMaxSize(),
            sectionHeader = {
                DartForecastHeader()
            },
            state = uiState.sectionedListState,
            onSectionToggle = onSectionToggle,
        ) { rowData ->
            DartForecastItemRow(
                destination = rowData.destination,
                eta = rowData.expectedAt,
                dueIn = if (rowData.dueIn == 0) {
                    stringResource(R.string.now)
                } else {
                    pluralStringResource(R.plurals.dart_minutes, rowData.dueIn, rowData.dueIn)
                },
                scheduledTime = rowData.scheduledAt,
                late = rowData.late,
            )
        }
    }
}

//@Preview
//@Composable
//private fun ForecastTabContentPreview() {
//    LuasTheme {
//        Surface {
//            ForecastTabContent(LuasLineEntity.GREEN, onRefreshAction = {}, onProgressChange = {})
//        }
//    }
//}
