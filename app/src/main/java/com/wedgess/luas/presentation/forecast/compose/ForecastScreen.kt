package com.wedgess.luas.presentation.forecast.compose

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.components.AnimatedTabContainer
import com.wedgess.luas.presentation.forecast.dart.compose.DartForecastContent
import com.wedgess.luas.presentation.forecast.luastab.compose.components.ForecastTabContent
import com.wedgess.luas.presentation.forecast.luastab.model.ForecastTab
import com.wedgess.luas.ui.theme.LuasTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ForecastScreen(
    transportType: TransportType,
    setRefreshAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    onProgressChange: (Float) -> Unit,
) {
    val forecastTabs = remember { ForecastTab.all() }
    val refreshActions = remember { mutableStateMapOf<ForecastTab, (() -> Unit)?>() }
    var currentTab by remember { mutableStateOf(forecastTabs.first()) }
    val currentRefreshAction by remember { derivedStateOf { refreshActions[currentTab] } }

    LaunchedEffect(currentTab, currentRefreshAction) {
        currentRefreshAction?.let(setRefreshAction)
    }

    Surface {
        if (transportType == TransportType.DART) {
            currentTab = ForecastTab.Dart
            DartForecastContent(
                onRefreshAction = { action ->
                    refreshActions.putIfAbsent(ForecastTab.Dart, action)
                },
                onProgressChange = { progress ->
                    if (ForecastTab.Dart == currentTab) onProgressChange(progress)
                },
            )
        } else {
            AnimatedTabContainer(
                modifier = modifier,
                tabItems = forecastTabs,
                indicatorColors = persistentListOf(Color(0xFF66BF63), Color(0xFFE53935)),
                onTabIndexChange = { index -> currentTab = forecastTabs[index] },
            ) { tab ->
                ForecastTabContent(
                    line = tab.line,
                    onRefreshAction = { action -> refreshActions.putIfAbsent(tab, action) },
                    onProgressChange = { progress -> if (tab == currentTab) onProgressChange(progress) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun ForecastScreenPreview() {
    LuasTheme {
        Surface {
            ForecastScreen(transportType = TransportType.LUAS, setRefreshAction = {}, onProgressChange = {})
        }
    }
}
