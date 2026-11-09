package com.wedgess.luas.presentation.components.stationpicker

import com.wedgess.luas.presentation.model.DropdownItem

data class StationGroups(
    val favourites: List<DropdownItem>,
    val recents: List<DropdownItem>,
    val all: List<DropdownItem>
)

/**
 * Groups stations for the picker. Without a search query there are favourites, then recents (not already favourites),
 * then every station. With a query only the matching stations are returned, in one flat list.
 */
fun groupStations(
    options: List<DropdownItem>,
    query: String,
    favourites: Set<String>,
    recents: List<String>
): StationGroups {
    val trimmed = query.trim()
    if (trimmed.isNotEmpty()) {
        return StationGroups(emptyList(), emptyList(), options.filter { it.text.contains(trimmed, ignoreCase = true) })
    }
    val byName = options.associateBy { it.text }
    return StationGroups(
        favourites = options.filter { it.text in favourites },
        recents = recents.filter { it !in favourites }.mapNotNull { byName[it] },
        all = options
    )
}
