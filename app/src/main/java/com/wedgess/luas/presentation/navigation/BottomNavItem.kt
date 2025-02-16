package com.wedgess.luas.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Tram
import androidx.compose.ui.graphics.vector.ImageVector
import com.wedgess.luas.R
import com.wedgess.luas.presentation.model.UiText
import kotlinx.serialization.Serializable

sealed class BottomNavItem<T>(
    @Serializable open val route: T,
    open val title: UiText,
    open val icon: ImageVector
) {
    data object Forecast : BottomNavItem<Screens.Forecast>(
        route = Screens.Forecast,
        UiText.StringResource(R.string.nav_title_forecast),
        Icons.Outlined.Tram
    )

    data object Map : BottomNavItem<Screens.Map>(
        route = Screens.Map,
        UiText.StringResource(R.string.nav_title_map),
        Icons.Outlined.Map
    )

    companion object {
        fun all() = listOf(Forecast, Map)
    }
}
