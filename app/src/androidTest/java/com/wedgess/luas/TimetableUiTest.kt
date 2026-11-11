package com.wedgess.luas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.timetable.ScheduledDeparture
import com.wedgess.luas.domain.timetable.TimetableDay
import com.wedgess.luas.domain.timetable.TimetableFeed
import com.wedgess.luas.domain.timetable.TimetableStation
import com.wedgess.luas.domain.timetable.TimetableZone
import com.wedgess.luas.presentation.timetable.TimetableUiState
import com.wedgess.luas.presentation.timetable.compose.TimetableScreen
import com.wedgess.luas.ui.theme.LuasTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TimetableUiTest {
    @get:Rule val compose = createComposeRule()
    private val date = LocalDate.of(2026, 11, 10)
    private val now = Instant.parse("2026-11-10T09:00:00Z")
    private val station = TimetableStation("abbey street", "Abbey Street")
    private val feed = TimetableFeed(TransportType.LUAS, now, now, date, date.plusDays(30), TimetableZone, 90_000)
    private val departures = listOf(
        ScheduledDeparture("past", "Tallaght", "Red", "Outbound", now.minusSeconds(3600), date),
        ScheduledDeparture("next", "The Point", "Red", "Inbound", now.plusSeconds(600), date)
    )
    private val state = TimetableUiState(
        date = date,
        now = now,
        feed = feed,
        stations = listOf(station),
        station = station,
        day = TimetableDay(true, departures),
        loading = false
    )

    @Test
    fun scheduleIsLabelledAndOffersNoLiveAlarmAction() {
        compose.setContent { LuasTheme { TimetableScreen(state, {}, {}, {}) } }
        compose.onNodeWithText("Scheduled — not live", substring = true).assertIsDisplayed()
        compose.onNodeWithText("09:10").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Set reminder").assertDoesNotExist()
        compose.onNodeWithText("08:00").assertDoesNotExist()
        compose.onNodeWithText("Whole day").assertIsDisplayed().performClick()
        compose.onNodeWithText("08:00").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun firstDownloadRequiresAnExplicitTap() {
        var downloaded = false
        compose.setContent {
            LuasTheme {
                TimetableScreen(
                    state.copy(feed = null, stations = emptyList()),
                    { downloaded = true },
                    {},
                    {}
                )
            }
        }
        compose.onNodeWithText("Download timetable").performClick()
        assertTrue(downloaded)
    }

    @Test
    fun failedUpdateKeepsTheSavedScheduleVisibleInDarkTheme() {
        compose.setContent {
            LuasTheme(darkTheme = true, dynamicColor = false) {
                TimetableScreen(state.copy(updateFailed = true), {}, {}, {})
            }
        }
        compose.onNodeWithText("Couldn’t update the timetable.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("09:10").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun unavailableDateDoesNotOfferAnInferredSchedule() {
        compose.setContent {
            LuasTheme {
                TimetableScreen(
                    state.copy(date = date.plusYears(1), day = TimetableDay(false, emptyList())),
                    {},
                    {},
                    {}
                )
            }
        }
        compose.onNodeWithText("No timetable published for this date.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("09:10").assertDoesNotExist()
    }

    @Test
    fun dartDeparturesAreGroupedUnderTheirPublishedDirections() {
        compose.setContent {
            LuasTheme {
                TimetableScreen(
                    state.copy(
                        transport = TransportType.DART,
                        feed = feed.copy(transport = TransportType.DART),
                        day = TimetableDay(
                            true,
                            listOf(
                                departures[1].copy(tripId = "south", direction = "Southbound", destination = "Bray"),
                                departures[1].copy(tripId = "north", direction = "Northbound", destination = "Howth")
                            )
                        )
                    ),
                    {},
                    {},
                    {}
                )
            }
        }
        compose.onNodeWithText("Northbound").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Howth").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Southbound").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Bray").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Northbound").performClick()
        compose.onNodeWithText("Howth").assertDoesNotExist()
        compose.onNodeWithText("Bray").assertIsDisplayed()
        compose.onNodeWithText("Northbound").performClick()
        compose.onNodeWithText("Howth").assertIsDisplayed()
    }

    @Test
    fun metadataIsAvailableOnDemandAndDoesNotFillTheScheduleViewport() {
        compose.setContent { LuasTheme { TimetableScreen(state, {}, {}, {}) } }
        compose.onNodeWithText("Published service dates:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Update timetable").assertDoesNotExist()
        val rowTop = compose.onNodeWithText("Outbound").fetchSemanticsNode().boundsInRoot.top
        val height = compose.onRoot().fetchSemanticsNode().boundsInRoot.height
        assertTrue("Direction cards should start in the top third", rowTop < height / 3)
        compose.onNodeWithContentDescription("Timetable information").performClick()
        compose.onNodeWithText("Published service dates:", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Update timetable").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun luasLineTabsSelectAStationOnTheChosenLineAndFilterThePicker() {
        val red = station.copy(lines = setOf("Red"))
        val green = TimetableStation("broombridge", "Broombridge", setOf("Green"))
        var shown by mutableStateOf(state.copy(station = red, stations = listOf(red, green)))
        compose.setContent {
            LuasTheme {
                TimetableScreen(shown, {}, { selected ->
                    shown = shown.copy(
                        station = selected,
                        day = TimetableDay(true, listOf(departures[1].copy(line = selected.lines.single())))
                    )
                }, {})
            }
        }
        compose.onNodeWithText("Red").assertIsSelected()
        compose.onNodeWithText("Green").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals("broombridge", shown.station?.id) }
        compose.onNodeWithContentDescription("Station: Broombridge").performClick()
        compose.onNodeWithText("Abbey Street").assertDoesNotExist()
        compose.onAllNodesWithText("Broombridge", useUnmergedTree = true).onLast().assertIsDisplayed()
    }
}
