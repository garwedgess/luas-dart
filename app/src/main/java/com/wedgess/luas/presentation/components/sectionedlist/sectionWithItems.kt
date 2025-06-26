package com.wedgess.luas.presentation.components.sectionedlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wedgess.luas.presentation.components.sectionedlist.model.Section

@OptIn(ExperimentalFoundationApi::class)
fun <T> LazyListScope.sectionWithItems(
    section: Section<T>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    sectionHeader: @Composable (LazyItemScope.() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    stickyHeader(key = section.id) {
        Column(
            modifier = Modifier.animateContentSize(
                animationSpec = tween(300),
            ),
        ) {
            SectionHeader(
                title = section.title,
                subTitle = section.subTitle,
                isExpanded = isExpanded,
                onToggle = onToggle,
                showIcon = section.items.isNotEmpty()
            )
            if (isExpanded && section.items.isNotEmpty()) {
                sectionHeader?.invoke(this@stickyHeader)
            }
        }
    }

    items(
        items = section.items,
        key = { it.id },
    ) { item ->
        AnimatedVisibility(
            visible = isExpanded,
            modifier = if (section.items.size > 1) {
                Modifier.animateItem()
            } else {
                Modifier
            },
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(300)),
        ) {
            itemContent(item.content)
        }
    }
}

fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}
