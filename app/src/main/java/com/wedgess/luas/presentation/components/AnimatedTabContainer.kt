package com.wedgess.luas.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import com.wedgess.luas.presentation.model.TabItem
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun <T : TabItem> AnimatedTabContainer(
    tabItems: List<T>,
    modifier: Modifier = Modifier,
    indicatorColors: List<Color> = listOf(MaterialTheme.colorScheme.primary),
    onTabIndexChange: ((index: Int) -> Unit)? = null,
    onTabSelected: @Composable (T) -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = {
        tabItems.size
    })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                onTabIndexChange?.invoke(page)
            }
    }

    val indicatorColor by animateColorAsState(
        targetValue = if (indicatorColors.size > 1) {
            indicatorColors[pagerState.currentPage]
        } else {
            indicatorColors.first()
        },
        label = "Tab Indicator Color"
    )

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = indicatorColor
                )
            }
        ) {
            tabItems.forEachIndexed { index, tab ->
                Tab(
                    selected = index == pagerState.currentPage,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                index,
                                animationSpec = tween(durationMillis = 1000)
                            )
                        }
                    },
                    text = { Text(tab.title.asString()) },
                    icon = tab.icon?.let { icon ->
                        @Composable {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.title.asString()
                            )
                        }
                    }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 0
        ) { page ->
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState
                    .currentPageOffsetFraction
                ).absoluteValue

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = lerp(
                            start = 0.85f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                        scaleX = scale
                        scaleY = scale

                        alpha = lerp(
                            start = 0.5f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                    }
            ) {
                onTabSelected(tabItems[page])
            }
        }
    }
}
