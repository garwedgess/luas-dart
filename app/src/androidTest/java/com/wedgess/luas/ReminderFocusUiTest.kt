package com.wedgess.luas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.components.sectionedlist.model.Section
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionItem
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionedListState
import com.wedgess.luas.presentation.forecast.dart.DartForecastTabContract
import com.wedgess.luas.presentation.forecast.dart.compose.DartListContent
import com.wedgess.luas.presentation.forecast.dart.model.DartForecastSectionRowData
import com.wedgess.luas.presentation.forecast.luastab.compose.dialogs.NotificationTimeDialog
import com.wedgess.luas.presentation.navigation.reminder.ReminderTarget
import com.wedgess.luas.ui.theme.LuasTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ReminderFocusUiTest {
    @get:Rule val compose = createComposeRule()
    private val now = Instant.parse("2026-11-10T09:00:00Z")
    private val date = LocalDate.of(2026, 11, 10)
    private val alarm = Alarm(
        id = "focus", requestCode = 1, kind = AlarmKind.SPECIFIC_DEPARTURE, state = AlarmState.FIRED,
        transport = TransportType.DART, stationCode = "PERSE", stationName = "Dublin Pearse",
        tripCode = "E807", direction = DepartureDirection.SOUTHBOUND, operatingDate = date,
        notifyMinutesBefore = 3, reminderAt = now, createdAt = now, updatedAt = now
    )
    private val row = DartForecastSectionRowData(
        trainCode = "E807",
        destination = "Bray",
        dueIn = 8,
        late = 0,
        expectedAt = "09:08",
        scheduledAt = "09:08",
        direction = DepartureDirection.SOUTHBOUND,
        operatingDate = date
    )
    private fun state() = DartForecastTabContract.UiState(
        fetchedAt = now,
        forecastStationCode = "PERSE",
        sectionedListState = SectionedListState(
            sections = persistentListOf(
                Section(
                    1L,
                    "Northbound",
                    "20 trains",
                    (0 until 20).map {
                        SectionItem(
                            "N$it",
                            row.copy(trainCode = "N$it", direction = DepartureDirection.NORTHBOUND)
                        )
                    }
                        .toImmutableList()
                ),
                Section(2L, "Southbound", "1 train", persistentListOf(SectionItem("E807", row)))
            ),
            expandedSections = persistentMapOf(2L to false)
        )
    )

    @Test
    fun reminderRevealsCollapsedDirectionAndBrieflyPulsesTheExactTrain() {
        compose.mainClock.autoAdvance = false
        var state by mutableStateOf(state())
        var completed = 0
        compose.setContent {
            LuasTheme {
                DartListContent(
                    state,
                    onSectionToggle = { id ->
                        state = state.copy(
                            sectionedListState = state.sectionedListState.copy(
                                expandedSections = persistentMapOf(id to true)
                            )
                        )
                    },
                    onStopSelect = {},
                    onRowClick = {},
                    onCancelAlarm = {},
                    onEvent = {},
                    reminderTarget = ReminderTarget("tap", alarm),
                    onReminderComplete = { _, unavailable ->
                        assertEquals(false, unavailable)
                        completed++
                    }
                )
            }
        }
        compose.mainClock.advanceTimeBy(300)
        compose.onNode(hasStateDescription("Opened from departure reminder")).assertIsDisplayed()
        compose.mainClock.advanceTimeBy(1_800)
        compose.onNode(hasStateDescription("Opened from departure reminder")).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, completed) }
        compose.runOnIdle { state = state.copy(fetchedAt = now.plusSeconds(40)) }
        compose.mainClock.advanceTimeBy(1_800)
        compose.runOnIdle { assertEquals(1, completed) }
    }

    @Test
    fun sameTrainCodeOnADifferentOperatingDateIsNotHighlighted() {
        var unavailable = false
        val wrongDate = state().copy(
            sectionedListState = SectionedListState(
                persistentListOf(
                    Section(
                        2L,
                        "Southbound",
                        "1 train",
                        persistentListOf(
                            SectionItem("E807", row.copy(operatingDate = date.plusDays(1)))
                        )
                    )
                )
            )
        )
        compose.setContent {
            LuasTheme {
                DartListContent(
                    wrongDate,
                    {},
                    {},
                    {},
                    {},
                    {},
                    reminderTarget = ReminderTarget("tap", alarm),
                    onReminderComplete = { _, missing -> unavailable = missing }
                )
            }
        }
        compose.onNode(hasStateDescription("Opened from departure reminder")).assertDoesNotExist()
        compose.runOnIdle { assertTrue(unavailable) }
    }

    @Test
    fun severalRemindersDoNotMoveDeparturesAndTheirSheetStillOffersCancellation() {
        var state by mutableStateOf(state())
        var cancelled: String? = null
        compose.setContent {
            LuasTheme { DartListContent(state, {}, {}, {}, { cancelled = it }, {}) }
        }
        val before = compose.onNodeWithText("Northbound").fetchSemanticsNode().boundsInRoot.top
        compose.runOnIdle {
            state = state.copy(
                activeAlarms = (0 until 5).map {
                    alarm.copy(id = "a$it", stationName = "Station $it", destination = "Bray")
                }.toImmutableList()
            )
        }
        val after = compose.onNodeWithText("Northbound").fetchSemanticsNode().boundsInRoot.top
        assertEquals(before, after, 1f)
        compose.onNodeWithText("5 reminders").performClick()
        compose.onNodeWithText("Departure reminders").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cancel reminder from Station 0 to Bray").performClick()
        compose.runOnIdle { assertEquals("a0", cancelled) }
    }

    @Test
    fun typedReminderLeadIsAppliedInsteadOfSilentlyKeepingTheWheelValue() {
        var minutes by mutableIntStateOf(8)
        var confirmed: Int? = null
        compose.setContent {
            LuasTheme {
                NotificationTimeDialog(minutes, 8, { minutes = it }, { confirmed = it }, {})
            }
        }
        compose.onNodeWithText("8", useUnmergedTree = true).performClick()
        val editor = compose.onNode(hasSetTextAction())
        editor.performTextReplacement("1")
        editor.performImeAction()
        compose.onNodeWithText("Set reminder").performClick()
        compose.runOnIdle { assertEquals(1, confirmed) }
    }
}
