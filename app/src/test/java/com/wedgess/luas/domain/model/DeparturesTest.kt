package com.wedgess.luas.domain.model

import com.wedgess.luas.domain.Polled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class DeparturesTest {

    private val fetchedAt = Instant.parse("2026-11-09T22:00:00Z")

    private fun departure(destination: String, expectedAt: Instant?) = Departure(
        transport = TransportType.LUAS,
        direction = DepartureDirection.INBOUND,
        destination = destination,
        dueMins = expectedAt?.let { 1 },
        expectedAt = expectedAt
    )

    private val departures = Departures(
        stationCode = "STS",
        stationName = "St. Stephen's Green",
        fetchedAt = fetchedAt,
        message = "",
        items = listOf(
            departure("Past", fetchedAt.plusSeconds(60)),
            departure("Future", fetchedAt.plusSeconds(600)),
            departure("Service message", null)
        )
    )

    @Test
    fun `live data is shown unchanged however old the fetch is`() {
        val polled = Polled(departures)

        assertEquals(departures, polled.displayable(fetchedAt.plusSeconds(3_600)))
    }

    @Test
    fun `failed refresh keeps data but drops departures whose expected time has passed`() {
        val polled = Polled(departures, error = Exception("offline"))

        val shown = polled.displayable(fetchedAt.plusSeconds(120))

        assertEquals(listOf("Future", "Service message"), shown?.items?.map { it.destination })
    }

    @Test
    fun `failed refresh hides data older than the maximum stale age`() {
        val polled = Polled(departures, error = Exception("offline"))

        assertNull(polled.displayable(fetchedAt.plus(MaxStaleAge).plusSeconds(1)))
    }

    @Test
    fun `failed refresh still shows data exactly at the maximum stale age`() {
        val polled = Polled(departures, error = Exception("offline"))

        assertNotNull(polled.displayable(fetchedAt.plus(MaxStaleAge)))
    }

    @Test
    fun `nothing is shown when no fetch ever succeeded`() {
        assertNull(Polled<Departures>(null, Exception("offline")).displayable(fetchedAt))
    }

    @Test
    fun `restored data is never live and is treated like data after a failed refresh`() {
        val cached = Polled(departures, isCached = true)

        val shown = cached.displayable(fetchedAt.plusSeconds(120))

        assertEquals(listOf("Future", "Service message"), shown?.items?.map { it.destination })
    }

    @Test
    fun `restored data older than the maximum stale age is not shown`() {
        val cached = Polled(departures, isCached = true)

        assertNull(cached.displayable(fetchedAt.plus(MaxStaleAge).plusSeconds(1)))
    }

    @Test
    fun `data fetched in the future is not shown because the clock has moved`() {
        val cached = Polled(departures.copy(fetchedAt = fetchedAt.plusSeconds(3_600)), isCached = true)

        assertNull(cached.displayable(fetchedAt))
    }

    @Test
    fun `data fetched slightly ahead of the clock is tolerated`() {
        val cached = Polled(departures.copy(fetchedAt = fetchedAt.plusSeconds(60)), isCached = true)

        assertNotNull(cached.displayable(fetchedAt))
    }
}
