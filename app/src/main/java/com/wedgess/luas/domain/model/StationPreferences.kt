package com.wedgess.luas.domain.model

/** Favourite and recently used station names for one transport. Recents are ordered most recent first. */
data class StationPreferences(
    val favourites: Set<String> = emptySet(),
    val recents: List<String> = emptyList()
)
