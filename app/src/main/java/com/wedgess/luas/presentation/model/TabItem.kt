package com.wedgess.luas.presentation.model

import androidx.compose.ui.graphics.vector.ImageVector

abstract class TabItem(
    open val title: UiText,
    open val icon: ImageVector?
)