package com.wedgess.luas.data.mapper

import com.wedgess.luas.data.model.DartDirectionData
import com.wedgess.luas.data.model.DartLocationTypeData
import com.wedgess.luas.data.model.DartStationForecastResponseData
import com.wedgess.luas.data.model.DartStationForecastResponseData.DartStationForecastData
import com.wedgess.luas.data.model.LuasDirectionKeyData
import com.wedgess.luas.data.model.LuasStopForcastResponseData
import com.wedgess.luas.domain.model.DepartureDirection
import com.wedgess.luas.domain.model.TransportType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DepartureMappersTest {

    private val fetchedAt = Instant.parse("2026-11-09T22:00:00Z")

    private fun tram(destination: String, dueMins: String) =
        LuasStopForcastResponseData.DirectionData.TramData(dueMins = dueMins, destination = destination)

    private fun luas(vararg directions: LuasStopForcastResponseData.DirectionData) = LuasStopForcastResponseData(
        created = "2026-11-09T22:00:00",
        stop = "Stillorgan",
        stopAbv = "STI",
        message = "Green Line services operating normally",
        direction = directions.toList()
    )

    @Test
    fun `luas trams become departures expected dueMins after the fetch with no vehicle identity`() {
        val response = luas(
            LuasStopForcastResponseData.DirectionData(
                name = LuasDirectionKeyData.INBOUND,
                tram = listOf(tram("Broombridge", "2"), tram("Parnell", "DUE"))
            ),
            LuasStopForcastResponseData.DirectionData(
                name = LuasDirectionKeyData.OUTBOUND,
                tram = listOf(tram("Brides Glen", "17"))
            )
        )

        val departures = response.toDepartures(fetchedAt)

        assertEquals("STI", departures.stationCode)
        assertEquals(fetchedAt, departures.fetchedAt)
        assertEquals(3, departures.items.size)
        val first = departures.items.first()
        assertEquals(TransportType.LUAS, first.transport)
        assertEquals(DepartureDirection.INBOUND, first.direction)
        assertEquals(2, first.dueMins)
        assertEquals(fetchedAt.plusSeconds(120), first.expectedAt)
        assertNull("Luas exposes no vehicle identity", first.tripCode)
        assertNull(first.scheduledAt)
        assertEquals(0, departures.items[1].dueMins) // "DUE"
        assertEquals(DepartureDirection.OUTBOUND, departures.items[2].direction)
    }

    @Test
    fun `luas placeholder rows are dropped and service messages carry no countdown`() {
        val response = luas(
            LuasStopForcastResponseData.DirectionData(
                name = LuasDirectionKeyData.INBOUND,
                tram = listOf(
                    tram("No trams forecast", "0"),
                    tram("See news for information", "0"),
                    tram("Green Line services operating with delays", "1")
                )
            )
        )

        val items = response.toDepartures(fetchedAt).items

        assertEquals(1, items.size)
        assertNull(items.single().dueMins)
        assertNull(items.single().expectedAt)
    }

    private fun dart(
        trainCode: String = "E846 ",
        dueIn: Int = 22,
        late: Int = 1,
        trainDate: String = "09 Nov 2026",
        direction: DartDirectionData = DartDirectionData.NORTHBOUND
    ) = DartStationForecastData(
        serverTime = "2026-11-09T22:36:58.573",
        trainCode = trainCode,
        stationFullName = "Malahide",
        stationCode = "MHIDE",
        queryTime = "22:36:58",
        trainDate = trainDate,
        origin = "Bray",
        destination = "Malahide",
        originTime = "21:39",
        destinationTime = "22:57",
        status = "En Route",
        lastLocation = "Arrived Clontarf Road",
        dueIn = dueIn,
        late = late,
        expArrival = "22:58",
        expDepart = "00:00",
        schArrival = "22:57",
        schDepart = "00:00",
        direction = direction,
        trainType = "DART",
        locationType = DartLocationTypeData.DESTINATION
    )

    @Test
    fun `dart trains carry a trimmed train code, operating date and schedule derived from lateness`() {
        val departures = DartStationForecastResponseData(listOf(dart())).toDepartures(fetchedAt)

        val departure = departures.items.single()
        assertEquals("MHIDE", departures.stationCode)
        assertEquals("E846", departure.tripCode)
        assertEquals(LocalDate.of(2026, 11, 9), departure.operatingDate)
        assertEquals(22, departure.dueMins)
        assertEquals(fetchedAt.plusSeconds(22 * 60L), departure.expectedAt)
        assertEquals(fetchedAt.plusSeconds(21 * 60L), departure.scheduledAt)
        assertEquals(1, departure.lateMins)
        assertEquals(DepartureDirection.NORTHBOUND, departure.direction)
    }

    @Test
    fun `an early running dart train is scheduled after it is expected`() {
        val departure = DartStationForecastResponseData(listOf(dart(late = -2))).toDepartures(fetchedAt).items.single()

        assertEquals(departure.expectedAt!!.plusSeconds(120), departure.scheduledAt)
    }

    @Test
    fun `an unparseable dart train date leaves the operating date unknown instead of failing`() {
        val departure = DartStationForecastResponseData(listOf(dart(trainDate = "not a date"))).toDepartures(fetchedAt)
            .items.single()

        assertNull(departure.operatingDate)
    }

    @Test
    fun `an empty dart response maps to no departures`() {
        val departures = DartStationForecastResponseData(emptyList()).toDepartures(fetchedAt)

        assertEquals(emptyList<Any>(), departures.items)
    }
}
