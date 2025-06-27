package com.wedgess.luas.data.mapper

import com.wedgess.luas.data.model.DartDirectionData
import com.wedgess.luas.data.model.DartLocationTypeData
import com.wedgess.luas.data.model.DartStationForecastResponseData
import com.wedgess.luas.domain.model.DartDirectionEntity
import com.wedgess.luas.domain.model.DartLocationTypeEntity
import com.wedgess.luas.domain.model.DartStationForecastEntity

fun DartStationForecastResponseData.toEntity() = this.stationData.map {
    DartStationForecastEntity(
        serverTime = it.serverTime,
        trainCode = it.trainCode,
        stationFullName = it.stationFullName,
        stationCode = it.stationCode,
        queryTime = it.queryTime,
        trainDate = it.trainDate,
        origin = it.origin,
        destination = it.destination,
        originTime = it.originTime,
        destinationTime = it.destinationTime,
        status = it.status,
        lastLocation = it.lastLocation,
        dueIn = it.dueIn,
        late = it.late,
        expArrival = it.expArrival,
        expDepart = it.expDepart,
        schArrival = it.schArrival,
        schDepart = it.schDepart,
        direction = it.direction.toEntity(),
        trainType = it.trainType,
        locationType = it.locationType.toEntity(),
    )
}

fun DartDirectionData.toEntity() = when (this) {
    DartDirectionData.NORTHBOUND -> DartDirectionEntity.NORTHBOUND
    DartDirectionData.SOUTHBOUND -> DartDirectionEntity.SOUTHBOUND
    DartDirectionData.UNKNOWN -> DartDirectionEntity.UNKNOWN
}

fun DartLocationTypeData.toEntity() = when (this) {
    DartLocationTypeData.ORIGIN -> DartLocationTypeEntity.ORIGIN
    DartLocationTypeData.DESTINATION -> DartLocationTypeEntity.DESTINATION
    DartLocationTypeData.STOP -> DartLocationTypeEntity.STOP
}
