package com.wedgess.luas.presentation.forecast.dart

import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionedListState
import com.wedgess.luas.presentation.forecast.dart.model.DartForecastSectionRowData
import kotlinx.collections.immutable.persistentListOf

interface DartForecastTabContract {

    data class UiState(
        val sectionedListState: SectionedListState<DartForecastSectionRowData> = SectionedListState(
            sections = persistentListOf()
        ),
        val stations: List<DartStationEntity> = emptyList(),
        val selectedStation: DartStationEntity = DartStationEntity.initial(),
        val refreshProgress: Float = 0f
    )

    sealed interface Event {
        data class OnStationSelected(val station: DartStationEntity) : Event
        data object OnRefresh : Event
        data class OnToggleSection(val id: Long) : Event
    }
}
