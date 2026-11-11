package com.wedgess.luas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.forecast.luastab.LuasForecastTabContract
import com.wedgess.luas.presentation.forecast.luastab.compose.components.LuasTabListContent
import com.wedgess.luas.presentation.forecast.luastab.model.LuasDropdownItem
import com.wedgess.luas.ui.theme.LuasTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class LuasForecastUiTest {
    @get:Rule val compose = createComposeRule()
    private val now = Instant.parse("2026-11-11T06:53:00Z")
    private val state = LuasForecastTabContract.UiState(
        selectedStop = LuasDropdownItem("Phibsborough", false, false),
        forecastStationCode = "PHI",
        forecast = Departures(
            "PHI",
            "Phibsborough",
            now,
            "",
            listOf(
                Departure(TransportType.LUAS, DepartureDirection.OUTBOUND, "Brides Glen", 4, now.plusSeconds(240)),
                Departure(TransportType.LUAS, DepartureDirection.INBOUND, "Broombridge", 6, now.plusSeconds(360))
            )
        )
    )

    @Test
    fun collapsingOneDirectionKeepsTheOtherVisibleAndLiveRowsSetReminders() {
        var chosen: DepartureDirection? = null
        compose.setContent {
            LuasTheme {
                LuasTabListContent(state, {}, {}, { _, _ -> }, { _, _, direction, _ -> chosen = direction }, {})
            }
        }
        compose.onNodeWithText("Outbound").performClick()
        compose.onNodeWithText("Brides Glen").assertDoesNotExist()
        compose.onNodeWithText("Broombridge").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(DepartureDirection.INBOUND, chosen) }
        compose.onNodeWithText("Outbound").performClick()
        compose.onNodeWithText("Brides Glen").assertIsDisplayed()
    }

    @Test
    fun staleLuasRowsShowAbsoluteTimesAndCannotSetReminders() {
        var clicked = false
        compose.setContent {
            LuasTheme {
                LuasTabListContent(
                    state.copy(isStale = true),
                    {},
                    {},
                    { _, _ -> },
                    { _, _, _, _ -> clicked = true },
                    {}
                )
            }
        }
        compose.onNodeWithText("~06:59").assertIsDisplayed()
        compose.onNodeWithText("6 mins").assertDoesNotExist()
        compose.onNodeWithText("Broombridge").performClick()
        compose.runOnIdle { assertEquals(false, clicked) }
    }

    @Test
    fun activeReminderFillsItsDepartureIconAndCancellationClearsItImmediately() {
        val alarm = Alarm(
            id = "a", requestCode = 1, kind = AlarmKind.SPECIFIC_DEPARTURE, state = AlarmState.ACTIVE,
            transport = TransportType.LUAS, stationCode = "PHI", stationName = "Phibsborough",
            direction = DepartureDirection.INBOUND, destination = "Broombridge", notifyMinutesBefore = 3,
            targetExpectedAt = now.plusSeconds(360), targetObservedAt = now,
            reminderAt = now.plusSeconds(180), createdAt = now, updatedAt = now
        )
        var shown by mutableStateOf(state.copy(activeAlarms = persistentListOf(alarm)))
        compose.setContent {
            LuasTheme { LuasTabListContent(shown, {}, {}, { _, _ -> }, { _, _, _, _ -> }, {}) }
        }
        compose.onNodeWithContentDescription("Reminder set", useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { shown = shown.copy(activeAlarms = persistentListOf()) }
        compose.onNodeWithContentDescription("Reminder set", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Broombridge").assertIsDisplayed()
    }
}
