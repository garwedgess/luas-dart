package com.wedgess.luas.presentation.forecast.dart

import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionedListState
import com.wedgess.luas.presentation.forecast.dart.model.DartDropdownItem
import com.wedgess.luas.presentation.forecast.dart.model.DartForecastSectionRowData
import com.wedgess.luas.presentation.model.DropdownItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

interface DartForecastTabContract {

    data class UiState(
        val sectionedListState: SectionedListState<DartForecastSectionRowData> = SectionedListState(
            sections = persistentListOf()
        ),
        val stations: ImmutableList<DartStationEntity> = persistentListOf(),
        val selectedStation: DropdownItem = DartDropdownItem.default,
        val refreshProgress: Float = 0f,
        val dropdownOptions: ImmutableList<DropdownItem> = persistentListOf()
    )

    sealed interface Event {
        data class OnStationSelected(val stationName: String) : Event
        data object OnRefresh : Event
        data class OnToggleSection(val id: Long) : Event
    }
}
