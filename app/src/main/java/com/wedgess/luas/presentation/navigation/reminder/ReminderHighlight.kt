package com.wedgess.luas.presentation.navigation.reminder

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.wedgess.luas.R
import kotlinx.coroutines.delay

private const val HIGHLIGHT_DURATION_MILLIS = 1_400L
private const val PULSE_LEG_MILLIS = 230
private const val PULSE_ALPHA = 0.3f
private const val PULSE_COUNT = 3

/** Reveal first, then pulse once. Repeated polls cannot indefinitely replay a consumed request. */
@Composable
fun rememberReminderHighlight(
    target: ReminderTarget?,
    stale: Boolean,
    index: Int?,
    listState: LazyListState,
    onComplete: (String, Boolean) -> Unit
): Boolean {
    val relevant = target
    var active by remember(relevant?.token) { mutableStateOf(false) }
    val complete by rememberUpdatedState(onComplete)
    LaunchedEffect(relevant?.token, index, stale) {
        val request = relevant ?: return@LaunchedEffect
        if (index == null) {
            // A cache may omit a departure that is still loading from the live API.
            if (!stale) complete(request.token, true)
            return@LaunchedEffect
        }
        listState.scrollToItem(index)
        // The sticky station/direction header must not cover the revealed departure.
        val stickyHeader = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index < index }?.size ?: 0
        if (stickyHeader > 0) listState.scrollToItem(index, -stickyHeader)
        active = true
        delay(HIGHLIGHT_DURATION_MILLIS)
        active = false
        complete(request.token, false)
    }
    return active
}

/** Torrentbox-style primary colour pulse, limited to three gentle cycles. */
@Composable
fun Modifier.reminderHighlight(active: Boolean): Modifier {
    val alpha = remember { Animatable(0f) }
    val colour = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.reminder_highlighted)
    LaunchedEffect(active) {
        alpha.snapTo(0f)
        if (active) {
            repeat(PULSE_COUNT) {
                alpha.animateTo(PULSE_ALPHA, tween(PULSE_LEG_MILLIS))
                alpha.animateTo(0f, tween(PULSE_LEG_MILLIS))
            }
        }
    }
    return drawWithContent {
        drawContent()
        if (active) drawRect(colour.copy(alpha = alpha.value))
    }.then(if (active) Modifier.semantics { stateDescription = description } else Modifier)
}
