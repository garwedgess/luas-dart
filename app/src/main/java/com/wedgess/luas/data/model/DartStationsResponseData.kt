package com.wedgess.luas.data.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("ArrayOfObjStation", namespace = "http://api.irishrail.ie/realtime/", prefix = "")
data class DartStationsResponseData(
    @XmlElement(true)
    val stations: List<DartStationData>
) {
    @Serializable
    @XmlSerialName("objStation")
    data class DartStationData(
        @XmlElement(true)
        @XmlSerialName("StationDesc")
        val stationDesc: String,

        @XmlElement(true)
        @XmlSerialName("StationAlias")
        val stationAlias: String? = null,

        @XmlElement(true)
        @XmlSerialName("StationLatitude")
        val stationLatitude: Double,

        @XmlElement(true)
        @XmlSerialName("StationLongitude")
        val stationLongitude: Double,

        @XmlElement(true)
        @XmlSerialName("StationCode")
        val stationCode: String,

        @XmlElement(true)
        @XmlSerialName("StationId")
        val stationId: Int
    )
}
