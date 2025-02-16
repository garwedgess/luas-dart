package com.wedgess.luas.data.mapper

import com.wedgess.luas.data.model.DirectionKeyData
import com.wedgess.luas.data.model.StopForcastResponseData
import com.wedgess.luas.domain.model.ForcastEntity

fun StopForcastResponseData.toEntity() = ForcastEntity(
    createdAt = this.created,
    stop = this.stop,
    stopAbv = this.stopAbv,
    message = this.message,
    inboundTrams = this.direction.filter { it.name == DirectionKeyData.INBOUND }
        .flatMap { data -> data.tram.map { tram -> tram.toEntity() } }
        .filterNot { tram -> tram.destination == "No trams forecast" },
    outboundTrams = this.direction.filter { it.name == DirectionKeyData.OUTBOUND }
        .flatMap { data -> data.tram.map { tram -> tram.toEntity() } }
        .filterNot { tram -> tram.destination == "No trams forecast" }
)

fun StopForcastResponseData.DirectionData.TramData.toEntity() = ForcastEntity.TramEntity(
    dueMins = this.dueMins.toIntOrNull() ?: 0,
    destination = this.destination
)