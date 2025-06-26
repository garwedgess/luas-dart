package com.wedgess.luas.data.mapper

import com.wedgess.luas.data.model.LuasDirectionKeyData
import com.wedgess.luas.data.model.LuasStopForcastResponseData
import com.wedgess.luas.domain.model.LuasForcastEntity

fun LuasStopForcastResponseData.toEntity() = LuasForcastEntity(
    createdAt = this.created,
    stop = this.stop,
    stopAbv = this.stopAbv,
    message = this.message,
    inboundTrams = this.direction.filter { it.name == LuasDirectionKeyData.INBOUND }
        .flatMap { data -> data.tram.map { tram -> tram.toEntity() } }
        .filterNot { tram -> tram.destination == "No trams forecast" },
    outboundTrams = this.direction.filter { it.name == LuasDirectionKeyData.OUTBOUND }
        .flatMap { data -> data.tram.map { tram -> tram.toEntity() } }
        .filterNot { tram -> tram.destination == "No trams forecast" }
)

fun LuasStopForcastResponseData.DirectionData.TramData.toEntity() = LuasForcastEntity.TramEntity(
    dueMins = if (this.destination.contains(" service", ignoreCase = true)) {
        -1
    } else {
        this.dueMins.toIntOrNull() ?: 0
    },
    destination = this.destination
)
