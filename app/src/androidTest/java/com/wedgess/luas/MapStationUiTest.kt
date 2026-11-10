package com.wedgess.luas

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.wedgess.luas.domain.model.Departure
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.LocationEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.map.compose.components.MapStationPicker
import com.wedgess.luas.presentation.map.compose.components.MapStationSheet
import com.wedgess.luas.presentation.map.model.MapStationState
import com.wedgess.luas.presentation.model.UiResult
import com.wedgess.luas.ui.theme.LuasTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** Exercises the map sheet and its accessible station selector without depending on a remote map style. */
class MapStationUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val harcourt = LocationEntity.Luas("Harcourt", 53.33, -6.26, LuasLineEntity.GREEN, "HAR")
    private val cabra = harcourt.copy(name = "Cabra", stationCode = "CAB")
    private val forecast = Departures(
        "HAR",
        "Harcourt",
        Instant.parse("2026-11-10T09:00:00Z"),
        "",
        listOf(
            Departure(
                TransportType.LUAS,
                DepartureDirection.OUTBOUND,
                "Brides Glen",
                8,
                Instant.parse("2026-11-10T09:08:00Z")
            )
        )
    )

    @Test
    fun lightThemeSheetShowsStationDirectionAndLiveTimestamp() {
        compose.setContent {
            LuasTheme(darkTheme = false, dynamicColor = false) {
                MapStationSheet(MapStationState(harcourt, UiResult.Success(forecast)), {}, {})
            }
        }
        compose.onNodeWithText("Harcourt").assertIsDisplayed()
        compose.onNodeWithText("Outbound").assertIsDisplayed()
        compose.onNodeWithText("Brides Glen").assertIsDisplayed()
        compose.onNode(hasContentDescription("Live times, updated at 09:00")).assertIsDisplayed()
    }

    @Test
    fun darkThemeStoredResultsShowAbsoluteTimeAndNoLiveBadge() {
        compose.setContent {
            LuasTheme(darkTheme = true, dynamicColor = false) {
                MapStationSheet(MapStationState(harcourt, UiResult.Success(forecast), isStale = true), {}, {})
            }
        }
        compose.onNodeWithText("~09:08").assertIsDisplayed()
        compose.onNode(hasContentDescription("Live times, updated at", substring = true)).assertDoesNotExist()
        compose.onNodeWithText("8 mins").assertDoesNotExist()
    }

    @Test
    fun offlineSheetOffersRetry() {
        var retried = false
        compose.setContent {
            LuasTheme {
                MapStationSheet(MapStationState(harcourt, UiResult.Offline("")), {}, { retried = true })
            }
        }
        compose.onNodeWithText("You're offline").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(true, retried)
    }

    @Test
    fun stationSearchFiltersAndSelectsWithoutMapGestures() {
        var selected: LocationEntity? = null
        compose.setContent {
            LuasTheme { MapStationPicker(listOf(harcourt, cabra), { selected = it }) }
        }
        compose.onNodeWithText("Find a station").performClick()
        compose.onNodeWithText("Search stations").performTextInput("Cab")
        compose.onNodeWithText("Harcourt").assertDoesNotExist()
        compose.onNodeWithText("Cabra").performClick()
        assertEquals("CAB", selected?.stationCode)
    }
}
