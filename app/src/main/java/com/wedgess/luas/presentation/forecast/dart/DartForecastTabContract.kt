package com.wedgess.luas.presentation.forecast.dart

import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionedListState
import com.wedgess.luas.presentation.forecast.dart.model.DartDropdownItem
import com.wedgess.luas.presentation.forecast.dart.model.DartForecastSectionRowData
import com.wedgess.luas.presentation.model.DropdownItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant

interface DartForecastTabContract {

    data class UiState(
        val sectionedListState: SectionedListState<DartForecastSectionRowData> = SectionedListState(
            sections = persistentListOf()
        ),
        val stations: ImmutableList<DartStationEntity> = persistentListOf(),
        val selectedStation: DropdownItem = DartDropdownItem.default,
        /** True when the latest refresh failed, so the rows are the last live result and show no countdown. */
        val isStale: Boolean = false,
        val fetchedAt: Instant = Instant.EPOCH,
        val dropdownOptions: ImmutableList<DropdownItem> = persistentListOf()
    )

    sealed interface Event {
        data class OnStationSelected(val stationName: String) : Event
        data object OnRefresh : Event
        data class OnToggleSection(val id: Long) : Event
    }

    companion object {
        const val REFRESH_INTERVAL_MILLIS = 40_000L
    }
}
