package com.wedgess.luas.data.mapper

import com.wedgess.luas.DartStation
import com.wedgess.luas.data.model.DartStationsResponseData
import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.domain.model.LocationEntity

fun DartStationEntity.toDao() = DartStation(
    Id = this.id,
    Name = this.name,
    Code = this.code,
    Alias = this.alias,
    Latitude = this.latitude,
    Longitude = this.longitude
)

fun DartStation.toEntity() = DartStationEntity(
    id = this.Id,
    name = this.Name,
    code = this.Code,
    alias = this.Alias,
    latitude = this.Latitude,
    longitude = this.Longitude
)

fun DartStationsResponseData.toDao() = this.stations.map { station ->
    DartStation(
        Id = station.stationId.toLong(),
        Name = station.stationDesc,
        Code = station.stationCode,
        Alias = station.stationAlias,
        Latitude = station.stationLatitude,
        Longitude = station.stationLongitude
    )
}

fun DartStation.toLocationEntity() = LocationEntity.Dart(
    name = this.Name,
    latitude = this.Latitude,
    longitude = this.Longitude
)
