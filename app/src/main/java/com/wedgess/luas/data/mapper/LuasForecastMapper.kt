package com.wedgess.luas.data.mapper

import com.wedgess.luas.data.model.LuasDirectionKeyData
import com.wedgess.luas.data.model.LuasStopForcastResponseData
import com.wedgess.luas.domain.model.LuasForecastEntity

private const val NO_TRAMS_NEWS_MSG = "See news for information"
private const val NO_TRAMS_MSG = "No trams forecast"

fun LuasStopForcastResponseData.toEntity() = LuasForecastEntity(
    createdAt = this.created,
    stop = this.stop,
    stopAbv = this.stopAbv,
    message = this.message,
    inboundTrams = this.direction.filter { it.name == LuasDirectionKeyData.INBOUND }
        .flatMap { data -> data.tram.map { tram -> tram.toEntity() } }
        .filterNot { tram ->
            tram.destination == NO_TRAMS_MSG || tram.destination == NO_TRAMS_NEWS_MSG
        },
    outboundTrams = this.direction.filter { it.name == LuasDirectionKeyData.OUTBOUND }
        .flatMap { data -> data.tram.map { tram -> tram.toEntity() } }
        .filterNot { tram ->
            tram.destination == NO_TRAMS_MSG || tram.destination == NO_TRAMS_NEWS_MSG
        },
)

fun LuasStopForcastResponseData.DirectionData.TramData.toEntity() = LuasForecastEntity.TramEntity(
    dueMins = if (this.destination.contains(" service", ignoreCase = true)) {
        -1
    } else {
        this.dueMins.toIntOrNull() ?: 0
    },
    destination = this.destination,
)
