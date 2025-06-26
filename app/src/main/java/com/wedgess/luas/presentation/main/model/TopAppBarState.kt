package com.wedgess.luas.presentation.main.model

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import com.wedgess.luas.domain.model.TransportType

data class TopAppBarState(
    val title: String,
    val actions: @Composable (RowScope.() -> Unit)? = null,
    val hasProgress: Boolean = false,
    val transportType: TransportType = TransportType.LUAS
)
