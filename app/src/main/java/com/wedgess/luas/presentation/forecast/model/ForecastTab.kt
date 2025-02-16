package com.wedgess.luas.presentation.forecast.model

import com.wedgess.luas.R
import com.wedgess.luas.presentation.model.TabItem
import com.wedgess.luas.presentation.model.UiText

sealed class ForecastTab(override val title: UiText) :
    TabItem(title = title, icon = null) {

    data object GreenLine : ForecastTab(
        title = UiText.StringResource(R.string.forecast_tab_title_green_line),
    )

    data object RedLine : ForecastTab(
        title = UiText.StringResource(R.string.forecast_tab_title_red_line)
    )

    companion object {
        fun all() = listOf(GreenLine, RedLine)
    }
}
