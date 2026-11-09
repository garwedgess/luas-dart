package com.wedgess.luas.data.db.dao

import com.wedgess.luas.data.LuasDatabase
import com.wedgess.luas.data.mapper.toDepartures
import com.wedgess.luas.data.mapper.toSnapshot
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.domain.model.TransportType
import java.time.Duration
import javax.inject.Inject

/** How long snapshots are kept. This is storage retention only; whether one may still be shown is a freshness rule. */
val SnapshotRetention: Duration = Duration.ofHours(24)

class DepartureSnapshotDao @Inject constructor(private val db: LuasDatabase) {

    private val queries = db.departureSnapshotQueries

    /** Replaces the station's stored result with this one, so a station never holds a mix of two fetches. */
    fun save(transport: TransportType, stationCode: String, departures: Departures) = db.transaction {
        val (snapshot, items) = departures.toSnapshot(transport, stationCode)
        queries.deleteItems(snapshot.Transport, snapshot.StationCode)
        queries.upsertSnapshot(
            Transport = snapshot.Transport,
            StationCode = snapshot.StationCode,
            StationName = snapshot.StationName,
            Message = snapshot.Message,
            FetchedAtMillis = snapshot.FetchedAtMillis
        )
        items.forEach { item ->
            queries.insertItem(
                Transport = item.Transport,
                StationCode = item.StationCode,
                Position = item.Position,
                Direction = item.Direction,
                Destination = item.Destination,
                DueMins = item.DueMins,
                ExpectedAtMillis = item.ExpectedAtMillis,
                ScheduledAtMillis = item.ScheduledAtMillis,
                LateMins = item.LateMins,
                TripCode = item.TripCode,
                OperatingDate = item.OperatingDate,
                Status = item.Status,
                LastLocation = item.LastLocation
            )
        }
    }

    /** The stored result with its original fetch time. It is not checked for freshness here. */
    fun load(transport: TransportType, stationCode: String): Departures? = db.transactionWithResult {
        queries.selectSnapshot(transport.name, stationCode).executeAsOneOrNull()?.let { snapshot ->
            snapshot.toDepartures(queries.selectItems(transport.name, stationCode).executeAsList())
        }
    }

    /** Retention: removes snapshots fetched before the cut-off, whether or not they are fresh enough to show. */
    fun deleteFetchedBefore(fetchedBeforeMillis: Long) = db.transaction {
        queries.deleteItemsOfSnapshotsFetchedBefore(fetchedBeforeMillis)
        queries.deleteSnapshotsFetchedBefore(fetchedBeforeMillis)
    }
}
