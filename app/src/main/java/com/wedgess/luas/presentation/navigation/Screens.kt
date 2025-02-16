package com.wedgess.luas.presentation.navigation;

import kotlinx.serialization.Serializable

sealed interface Screens {
    @Serializable
    data object Forecast : Screens

    @Serializable
    data object Map : Screens
}