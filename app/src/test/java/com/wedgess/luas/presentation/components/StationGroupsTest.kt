package com.wedgess.luas.presentation.components

import com.wedgess.luas.presentation.components.stationpicker.groupStations
import com.wedgess.luas.presentation.forecast.dart.model.DartDropdownItem
import org.junit.Assert.assertEquals
import org.junit.Test

class StationGroupsTest {

    private val options = listOf("Bray", "Dalkey", "Howth", "Malahide").map { DartDropdownItem(it) }

    private fun names(items: List<com.wedgess.luas.presentation.model.DropdownItem>) = items.map { it.text }

    @Test
    fun `without a query favourites come first then recents then all stations`() {
        val groups = groupStations(options, "", setOf("Howth"), listOf("Bray", "Howth", "Dalkey"))

        assertEquals(listOf("Howth"), names(groups.favourites))
        assertEquals("a favourite is not repeated under recents", listOf("Bray", "Dalkey"), names(groups.recents))
        assertEquals(4, groups.all.size)
    }

    @Test
    fun `a search returns only matching stations, ignoring case`() {
        val groups = groupStations(options, " AL ", setOf("Howth"), listOf("Bray"))

        assertEquals(listOf("Dalkey", "Malahide"), names(groups.all))
        assertEquals(emptyList<String>(), names(groups.favourites))
        assertEquals(emptyList<String>(), names(groups.recents))
    }

    @Test
    fun `favourites and recents that are not in this list are ignored`() {
        val groups = groupStations(options, "", setOf("Ghost"), listOf("Ghost", "Bray"))

        assertEquals(emptyList<String>(), names(groups.favourites))
        assertEquals(listOf("Bray"), names(groups.recents))
    }

    @Test
    fun `a search with no match is empty`() {
        assertEquals(emptyList<String>(), names(groupStations(options, "zzz", emptySet(), emptyList()).all))
    }
}
