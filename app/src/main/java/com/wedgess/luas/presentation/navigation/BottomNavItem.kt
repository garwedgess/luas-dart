package com.wedgess.luas.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Tram
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.wedgess.luas.R
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.model.UiText
import kotlinx.serialization.Serializable

sealed class BottomNavItem<T>(
    @Serializable open val route: T,
    open val title: UiText,
    open val icon: ImageVector
) {
    data class Forecast(val transportType: TransportType) : BottomNavItem<Screens.Forecast>(
        route = Screens.Forecast,
        UiText.StringResource(R.string.nav_title_forecast),
        Icons.Outlined.Tram
    ) {
        @Composable
        fun dynamicIcon(): Painter {
            return when (transportType) {
                TransportType.LUAS -> painterResource(R.drawable.ic_tram)
                TransportType.DART -> painterResource(R.drawable.ic_train)
            }
        }
    }

    data object Map : BottomNavItem<Screens.Map>(
        route = Screens.Map,
        UiText.StringResource(R.string.nav_title_map),
        Icons.Outlined.Map
    )

    data object Alerts : BottomNavItem<Screens.Alerts>(
        route = Screens.Alerts,
        UiText.StringResource(R.string.nav_title_news),
        Icons.Outlined.Info
    )

    companion object {
        fun all(transportType: TransportType) = when (transportType) {
            TransportType.LUAS -> listOf(Forecast(transportType), Map, Alerts)
            TransportType.DART -> listOf(Forecast(transportType), Map)
        }
    }
}
