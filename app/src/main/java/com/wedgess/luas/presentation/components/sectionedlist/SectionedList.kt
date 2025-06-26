package com.wedgess.luas.presentation.components.sectionedlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionedListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> SectionedList(
    state: SectionedListState<T>,
    onSectionToggle: (sectionId: Long) -> Unit,
    modifier: Modifier = Modifier,
    sectionHeader: @Composable (LazyItemScope.() -> Unit)? = null,
    itemContent: @Composable (item: T) -> Unit,
) {

    LazyColumn(
        modifier = modifier,
        state = rememberLazyListState(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.sections.forEachIndexed { index, section ->
            val isExpanded = state.expandedSections[section.id] ?: true

            if (index != 0) {
                item(key = "divider_$index") {
                    HorizontalDivider(Modifier.fillMaxWidth())
                }
            }

            sectionWithItems(
                section = section,
                sectionHeader = sectionHeader,
                isExpanded = isExpanded,
                onToggle = { onSectionToggle(section.id) },
                itemContent = itemContent,
            )
        }
    }
}
