package com.wedgess.luas.presentation.main.model

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable

data class TopAppBarState(
    val title: String,
    val actions: @Composable (RowScope.() -> Unit)? = null,
    val hasProgress: Boolean = false
)
