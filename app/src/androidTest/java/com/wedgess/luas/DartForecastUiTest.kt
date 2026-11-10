package com.wedgess.luas

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.presentation.components.sectionedlist.model.Section
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionItem
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionedListState
import com.wedgess.luas.presentation.forecast.dart.DartForecastTabContract
import com.wedgess.luas.presentation.forecast.dart.compose.DartListContent
import com.wedgess.luas.presentation.forecast.dart.model.DartForecastSectionRowData
import com.wedgess.luas.ui.theme.LuasTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Runs the stateless DART list on a device with fixed (mock) state, so it does not need live trains. */
class DartForecastUiTest {

    @get:Rule
    val compose = createComposeRule()

    private val row = DartForecastSectionRowData(
        trainCode = "E134",
        destination = "Greystones",
        dueIn = 8,
        late = 0,
        expectedAt = "09:14",
        scheduledAt = "09:14",
        direction = DepartureDirection.SOUTHBOUND
    )

    private fun state(stale: Boolean) = DartForecastTabContract.UiState(
        sectionedListState = SectionedListState(
            sections = persistentListOf(
                Section(2L, "Southbound", "1 train", persistentListOf(SectionItem("E134", row)))
            )
        ),
        isStale = stale
    )

    private fun show(stale: Boolean, onRowClick: (DartForecastSectionRowData) -> Unit) = compose.setContent {
        LuasTheme {
            DartListContent(
                uiState = state(stale),
                onSectionToggle = {},
                onStopSelect = {},
                onRowClick = onRowClick,
                onCancelAlarm = {},
                onEvent = {}
            )
        }
    }

    @Test
    fun liveRowShowsNextDepartureAndIsClickable() {
        var clicked: DartForecastSectionRowData? = null
        show(stale = false) { clicked = it }

        compose.onNode(hasContentDescription("Next departure to Greystones", substring = true)).assertIsDisplayed()
        compose.onNode(hasContentDescription("Live times, updated at", substring = true)).assertIsDisplayed()
        // Index 1 is the row; index 0 is inside the next-departure header.
        compose.onAllNodesWithText("Greystones", useUnmergedTree = true)[1].performClick()
        assertEquals("E134", clicked?.trainCode)
    }

    @Test
    fun staleRowShowsApproximateTimeAndCannotSetAnAlarm() {
        var clicked = false
        show(stale = true) { clicked = true }

        compose.onNode(hasContentDescription("expected around 09:14, not live", substring = true)).assertIsDisplayed()
        compose.onNode(hasContentDescription("Live times, updated at", substring = true)).assertDoesNotExist()
        // Index 1 is the row; index 0 is inside the next-departure header.
        compose.onAllNodesWithText("Greystones", useUnmergedTree = true)[1].performClick()
        assertEquals(false, clicked)
    }
}
