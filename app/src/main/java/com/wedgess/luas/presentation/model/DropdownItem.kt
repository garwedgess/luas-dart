package com.wedgess.luas.presentation.model

import androidx.compose.ui.graphics.vector.ImageVector

data class DropdownItem(
    val text: String,
    val icons: List<ImageVector> = emptyList()
)
