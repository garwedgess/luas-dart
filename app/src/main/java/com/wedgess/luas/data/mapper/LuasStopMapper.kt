package com.wedgess.luas.data.mapper

import com.wedgess.luas.LuasStop
import com.wedgess.luas.data.model.LuasStopsResponseData
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.model.StationLocationEntity
import java.util.UUID

fun LuasStopEntity.toDao() = LuasStop(
    Id = UUID.randomUUID(),
    Name = this.name,
    Abbreviation = this.abbreviation,
    IsParkRide = this.isParkAndRide,
    IsCycleRide = this.isCycleAndRide,
    Latitude = this.latitude,
    Longitude = this.longitude,
    Line = this.line.fromEntity(),
)

fun LuasStop.toEntity() = LuasStopEntity(
    id = this.Id,
    name = this.Name,
    abbreviation = this.Abbreviation,
    isParkAndRide = this.IsParkRide,
    isCycleAndRide = this.IsCycleRide,
    latitude = this.Latitude,
    longitude = this.Longitude,
    line = this.Line.toEntity(),
)

fun LuasStopsResponseData.toDao() = this.line.flatMap { line ->
    line.stop.map { stop ->
        LuasStop(
            Id = UUID.randomUUID(),
            Name = stop.name,
            Abbreviation = stop.abrev,
            IsParkRide = stop.isParkRide == 1,
            IsCycleRide = stop.isCycleRide == 1,
            Latitude = stop.lat,
            Longitude = stop.long,
            Line = line.name,
        )
    }
}

fun LuasStop.toLocationEntity() = StationLocationEntity.LuasStationLocationEntity(
    name = this.Name,
    latitude = this.Latitude,
    longitude = this.Longitude,
    line = this.Line.toEntity()
)
