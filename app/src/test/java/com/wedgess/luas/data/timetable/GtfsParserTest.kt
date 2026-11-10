package com.wedgess.luas.data.timetable

import com.wedgess.luas.domain.model.TransportType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

class GtfsParserTest {
    @get:Rule val files = TemporaryFolder()
    private val parser = GtfsParser()

    @Test
    fun `rail import includes DART only and skips terminal boarding`() {
        val parsed = parser.parse(fixtureZip(files.newFile()), TransportType.DART)
        assertEquals(setOf("t1", "t2"), parsed.schedule.trips.map { it.id }.toSet())
        assertEquals(setOf("Dublin Connolly", "Bray"), parsed.schedule.stations.map { it.name }.toSet())
        assertEquals(2, parsed.schedule.times.size)
        assertEquals(90_600L, parsed.schedule.times.last().seconds)
        assertEquals(LocalDate.of(2026, 11, 13), parsed.schedule.services.dates.max())
    }

    @Test
    fun `Luas platform aliases are one station and other routes are ignored`() {
        val parsed = parser.parse(fixtureZip(files.newFile()), TransportType.LUAS)
        assertEquals(listOf("Abbey Street"), parsed.schedule.stations.map { it.name })
        assertEquals(2, parsed.schedule.times.size)
        assertEquals("Outbound", parsed.schedule.trips.single().direction)
    }

    @Test
    fun `quoted UTF8 fields newlines and BOM are supported`() {
        val file = fixtureZip(
            files.newFile(),
            mapOf(
                "stops.txt" to "\uFEFF\"stop_id\",stop_name\r\nc,\"Connolly, east\r\nplatform\"\r\nb,Bray (Daly)\r\n"
            )
        )
        val parsed = parser.parse(file, TransportType.DART)
        assertTrue(parsed.schedule.stations.any { it.name == "Connolly, east\r\nplatform" })
    }

    @Test
    fun `calendar dates alone can define service`() {
        val file = fixtureZip(
            files.newFile(),
            mapOf(
                "calendar.txt" to null,
                "calendar_dates.txt" to "service_id,date,exception_type\nweekday,20261110,1\nspecial,20261111,1\n"
            )
        )
        val services = parser.parse(file, TransportType.DART).schedule.services
        assertEquals(setOf("weekday"), services.active(LocalDate.of(2026, 11, 10)))
        assertFalse(services.covers(LocalDate.of(2026, 11, 12)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `frequency trips are rejected instead of presenting template times as departures`() {
        parser.parse(
            fixtureZip(
                files.newFile(),
                mapOf(
                    "frequencies.txt" to "trip_id,start_time,end_time,headway_secs\nt1,07:00:00,09:00:00,300\n"
                )
            ),
            TransportType.DART
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `truncated quoted CSV is rejected`() {
        val file = fixtureZip(files.newFile(), mapOf("stops.txt" to "stop_id,stop_name\nc,\"bad\n"))
        parser.parse(file, TransportType.DART)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `malformed time cannot silently become midnight`() {
        parseGtfsTime("24:65:00")
    }

    @Test
    fun `multi-day times are preserved without modulo`() {
        assertEquals(177_300L, parseGtfsTime("49:15:00"))
    }

    @Test
    fun `CSV preserves escaped quotes empty fields and trailing record`() {
        val rows = mutableListOf<Map<String, String>>()
        GtfsCsv("a,b,c\r\n\"a\"\"b\",,x\r\nlast,value,".reader()).forEach { rows.add(it) }
        assertEquals("a\"b", rows[0]["a"])
        assertEquals("", rows[0]["b"])
        assertEquals("", rows[1]["c"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `CSV rejects changed column counts`() {
        GtfsCsv("a,b\nx\n".reader()).forEach {}
    }
}
