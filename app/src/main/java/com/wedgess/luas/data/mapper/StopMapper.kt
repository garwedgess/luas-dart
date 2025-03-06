package com.wedgess.luas.data.mapper

import com.wedgess.luas.Stop
import com.wedgess.luas.data.model.StopsResponseData
import com.wedgess.luas.domain.model.StopEntity
import java.util.UUID

fun StopEntity.toDao() = Stop(
    Id = UUID.randomUUID(),
    Name = this.name,
    Abbreviation = this.abbreviation,
    IsParkRide = this.isParkAndRide,
    IsCycleRide = this.isCycleAndRide,
    Latitude = this.latitude,
    Longitude = this.longitude,
    Line = this.line.fromEntity()
)

fun Stop.toEntity() = StopEntity(
    id = this.Id,
    name = this.Name,
    abbreviation = this.Abbreviation,
    isParkAndRide = this.IsParkRide,
    isCycleAndRide = this.IsCycleRide,
    latitude = this.Latitude,
    longitude = this.Longitude,
    line = this.Line.toEntity()
)

fun StopsResponseData.toDao() = this.line.flatMap { line ->
    line.stop.map { stop ->
        Stop(
            Id = UUID.randomUUID(),
            Name = stop.name,
            Abbreviation = stop.abrev,
            IsParkRide = stop.isParkRide == 1,
            IsCycleRide = stop.isCycleRide == 1,
            Latitude = stop.lat,
            Longitude = stop.long,
            Line = line.name
        )
    }
}
