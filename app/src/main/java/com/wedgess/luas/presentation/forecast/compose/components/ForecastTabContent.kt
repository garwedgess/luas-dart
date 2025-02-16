package com.wedgess.luas.presentation.forecast.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wedgess.luas.R
import com.wedgess.luas.di.ForecastTabViewModelFactory
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.StopEntity
import com.wedgess.luas.presentation.DropdownTextField
import com.wedgess.luas.presentation.EmptyContent
import com.wedgess.luas.presentation.ErrorContent
import com.wedgess.luas.presentation.LoadingContent
import com.wedgess.luas.presentation.forecast.ForecastContract
import com.wedgess.luas.presentation.forecast.viewmodel.ForecastViewModel
import com.wedgess.luas.presentation.model.Compose
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun ForecastTabContent(line: LuasLineEntity) {
    val stopsViewModel: ForecastViewModel = hiltViewModel(
        key = line.name,
        creationCallback = { factory: ForecastTabViewModelFactory ->
            factory.create(line)
        }
    )
    val uiResult by stopsViewModel.uiResult.collectAsStateWithLifecycle()

    Surface {
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
            onSuccess = {
                TabListContent(
                    uiState = it,
                    onStopSelected = {
                        stopsViewModel.onEvent((ForecastContract.Event.OnStopSelected(it)))
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabListContent(uiState: ForecastContract.UiState, onStopSelected: (StopEntity) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        stickyHeader {
            DropdownTextField(
                modifier = Modifier.fillMaxWidth(),
                label = "Stop",
                valueFormatter = { item -> item.name },
                options = uiState.stops,
                selectedValue = uiState.selectedStop,
                onValueChange = onStopSelected
            )
        }
        item {
            TramDirectionHeader(
                title = stringResource(R.string.forecast_title_outbound),
                noTramsDue = uiState.forecast.outboundTrams.isEmpty()
            )
        }
        items(uiState.forecast.outboundTrams) { tram ->
            ForecastItemRow(
                dueInMins = tram.dueMins,
                destination = tram.destination
            )
        }

        item {
            TramDirectionHeader(
                title = stringResource(R.string.forecast_title_inbound),
                noTramsDue = uiState.forecast.inboundTrams.isEmpty()
            )
        }
        items(uiState.forecast.inboundTrams) { tram ->
            ForecastItemRow(
                dueInMins = tram.dueMins,
                destination = tram.destination
            )
        }
    }
}

@Preview
@Composable
private fun ForecastTabContentPreview() {
    LuasTheme {
        Surface {
            ForecastTabContent(LuasLineEntity.GREEN)
        }
    }
}
