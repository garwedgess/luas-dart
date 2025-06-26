package com.wedgess.luas.presentation.forecast.luastab.model

import com.wedgess.luas.R
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.presentation.model.TabItem
import com.wedgess.luas.presentation.model.UiText
import kotlinx.collections.immutable.persistentListOf

sealed class ForecastTab(override val title: UiText, val line: LuasLineEntity) :
    TabItem(title = title, icon = null) {

    data object GreenLine : ForecastTab(
        title = UiText.StringResource(R.string.forecast_tab_title_green_line),
        line = LuasLineEntity.GREEN
    )

    data object RedLine : ForecastTab(
        title = UiText.StringResource(R.string.forecast_tab_title_red_line),
        line = LuasLineEntity.RED
    )

    data object Dart : ForecastTab(
        title = UiText.StringResource(R.string.forecast_tab_title_red_line),
        line = LuasLineEntity.RED
    )

    companion object {
        fun all() = persistentListOf(GreenLine, RedLine)
    }
}
