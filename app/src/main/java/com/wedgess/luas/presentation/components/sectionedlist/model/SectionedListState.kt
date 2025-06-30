package com.wedgess.luas.presentation.components.sectionedlist.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

data class SectionedListState<T>(
    val sections: ImmutableList<Section<T>>,
    val expandedSections: ImmutableMap<Long, Boolean> = persistentMapOf()
)
