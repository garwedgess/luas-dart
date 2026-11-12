package com.wedgess.luas.presentation.components

import com.wedgess.luas.domain.model.StationPreferences
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.components.stationpicker.forOptions
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

    @Test
    fun `stored Forecast favourites and recents match timetable names and aliases`() {
        val shared = StationPreferences(
            favourites = setOf("Connolly", "Dún Laoghaire Mallin", "Unavailable"),
            recents = listOf(" HOWTH ", "Sandycove and Glasthule", "Missing")
        )
        val matched = shared.forOptions(
            TransportType.DART,
            listOf("Dublin Connolly", "Dun Laoghaire", "Howth", "Sandycove")
        )
        assertEquals(setOf("Dublin Connolly", "Dun Laoghaire"), matched.favourites)
        assertEquals(listOf("Howth", "Sandycove"), matched.recents)
    }

    @Test
    fun `station matching does not combine neighbouring stations or duplicate aliases`() {
        val matched = StationPreferences(
            favourites = setOf("Connolly"),
            recents = listOf("Connolly", "Dublin Connolly")
        ).forOptions(TransportType.DART, listOf("Dublin Connolly", "Howth", "Howth Junction"))
        assertEquals(setOf("Dublin Connolly"), matched.favourites)
        assertEquals(listOf("Dublin Connolly"), matched.recents)
    }
}
