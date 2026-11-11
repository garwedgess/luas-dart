package com.wedgess.luas

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.wedgess.luas.presentation.forecast.compose.ForecastConnectivityContent
import com.wedgess.luas.ui.theme.LuasTheme
import org.junit.Rule
import org.junit.Test

class ForecastConnectivityUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun offlineBannerAppearsOverExistingContentAndDisappearsImmediatelyOnReconnect() {
        var online by mutableStateOf(true)
        compose.setContent {
            LuasTheme {
                ForecastConnectivityContent(online) { Text("Existing departures") }
            }
        }
        compose.onNodeWithText("Auto refresh paused", substring = true).assertDoesNotExist()
        compose.runOnIdle { online = false }
        compose.onNodeWithText("Auto refresh paused", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Existing departures").assertIsDisplayed()
        compose.runOnIdle { online = true }
        compose.onNodeWithText("Auto refresh paused", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Existing departures").assertIsDisplayed()
    }
}
