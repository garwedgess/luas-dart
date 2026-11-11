package com.wedgess.luas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.wedgess.luas.presentation.forecast.compose.ForecastRefreshCycle
import com.wedgess.luas.presentation.forecast.compose.actions.ForecastRefreshButton
import com.wedgess.luas.ui.theme.LuasTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ForecastRefreshUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun toolbarRingCountsDownAndRestartsOnNewDataWhileRefreshStaysClickable() {
        compose.mainClock.autoAdvance = false
        val now = Instant.parse("2026-11-11T07:00:00Z")
        val clock = Clock.fixed(now, ZoneOffset.UTC)
        var cycle by mutableStateOf<ForecastRefreshCycle?>(ForecastRefreshCycle(now.minusSeconds(5), 15_000))
        var clicked = false
        compose.setContent { LuasTheme { ForecastRefreshButton(cycle, { clicked = true }, clock = clock) } }
        compose.mainClock.advanceTimeBy(64)
        val initial = progress()
        assertTrue("Ring reflects the elapsed refresh interval: $initial", initial in 0.4f..0.75f)
        compose.mainClock.advanceTimeBy(3_000)
        assertTrue(progress() < initial)
        compose.onNodeWithContentDescription("Refresh").performClick()
        compose.runOnIdle { assertTrue(clicked) }
        compose.runOnIdle { cycle = ForecastRefreshCycle(now, 15_000) }
        compose.mainClock.advanceTimeBy(64)
        assertTrue("A fresh response restarts the ring", progress() > 0.85f)
        compose.runOnIdle { cycle = null }
        compose.mainClock.advanceTimeBy(64)
        assertEquals(0f, progress(), 0.001f)
    }

    private fun progress(): Float = compose.onNode(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
        useUnmergedTree = true
    ).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
}
