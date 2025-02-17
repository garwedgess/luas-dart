package com.wedgess.luas.presentation.forecast.compose

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.presentation.components.AnimatedTabContainer
import com.wedgess.luas.presentation.forecast.compose.components.ForecastTabContent
import com.wedgess.luas.presentation.forecast.model.ForecastTab
import com.wedgess.luas.ui.theme.LuasTheme

@Composable
fun ForecastScreen() {
    AnimatedTabContainer(
        tabItems = ForecastTab.all(),
        indicatorColors = listOf(Color(0xFF66BF63), Color(0xFFE53935))
    ) { tabType ->
        when (tabType) {
            ForecastTab.GreenLine -> ForecastTabContent(line = LuasLineEntity.GREEN)
            ForecastTab.RedLine -> ForecastTabContent(line = LuasLineEntity.RED)
        }
    }
}

@Preview
@Composable
private fun FoecastScreenPreview() {
    LuasTheme {
        Surface {
            ForecastScreen()
        }
    }
}
