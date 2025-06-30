package com.wedgess.luas.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import timber.log.Timber

@Serializable
@XmlSerialName("ArrayOfObjStationData", "http://api.irishrail.ie/realtime/", "")
data class DartStationForecastResponseData(
    @XmlElement(true)
    val stationData: List<DartStationForecastData>
) {

    @Serializable
    @XmlSerialName("objStationData")
    data class DartStationForecastData(
        @XmlElement(true)
        @XmlSerialName("Servertime")
        val serverTime: String,

        @XmlElement(true)
        @XmlSerialName("Traincode")
        val trainCode: String,

        @XmlElement(true)
        @XmlSerialName("Stationfullname")
        val stationFullName: String,

        @XmlElement(true)
        @XmlSerialName("Stationcode")
        val stationCode: String,

        @XmlElement(true)
        @XmlSerialName("Querytime")
        val queryTime: String,

        @XmlElement(true)
        @XmlSerialName("Traindate")
        val trainDate: String,

        @XmlElement(true)
        @XmlSerialName("Origin")
        val origin: String,

        @XmlElement(true)
        @XmlSerialName("Destination")
        val destination: String,

        @XmlElement(true)
        @XmlSerialName("Origintime")
        val originTime: String,

        @XmlElement(true)
        @XmlSerialName("Destinationtime")
        val destinationTime: String,

        @XmlElement(true)
        @XmlSerialName("Status")
        val status: String? = null,

        @XmlElement(true)
        @XmlSerialName("Lastlocation")
        val lastLocation: String? = null,

        @XmlElement(true)
        @XmlSerialName("Duein")
        val dueIn: Int,

        @XmlElement(true)
        @XmlSerialName("Late")
        val late: Int,

        @XmlElement(true)
        @XmlSerialName("Exparrival")
        val expArrival: String,

        @XmlElement(true)
        @XmlSerialName("Expdepart")
        val expDepart: String,

        @XmlElement(true)
        @XmlSerialName("Scharrival")
        val schArrival: String,

        @XmlElement(true)
        @XmlSerialName("Schdepart")
        val schDepart: String,

        @Serializable(with = DirectionSerializer::class)
        @XmlElement(true)
        @XmlSerialName("Direction")
        val direction: DartDirectionData,

        @XmlElement(true)
        @XmlSerialName("Traintype")
        val trainType: String,

        @Serializable(with = LocationTypeSerializer::class)
        @XmlElement(true)
        @XmlSerialName("Locationtype")
        val locationType: DartLocationTypeData
    )

    private object DirectionSerializer : KSerializer<DartDirectionData> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor(DartDirectionData::class::simpleName.name, PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): DartDirectionData {
            val direction = decoder.decodeString().lowercase()

            return when (direction) {
                DartDirectionData.NORTHBOUND.name.lowercase() -> DartDirectionData.NORTHBOUND
                DartDirectionData.SOUTHBOUND.name.lowercase() -> DartDirectionData.SOUTHBOUND
                else -> {
                    Timber.e("Unknown dart direction: $direction")
                    DartDirectionData.UNKNOWN
                }
            }
        }

        override fun serialize(encoder: Encoder, value: DartDirectionData) {
            throw IllegalArgumentException("Not supported")
        }
    }

    private object LocationTypeSerializer : KSerializer<DartLocationTypeData> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor(DartLocationTypeData::class::simpleName.name, PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): DartLocationTypeData {
            val locationType = decoder.decodeString()

            return when (locationType) {
                DartLocationTypeData.DESTINATION.key -> DartLocationTypeData.DESTINATION
                DartLocationTypeData.ORIGIN.key -> DartLocationTypeData.ORIGIN
                DartLocationTypeData.STOP.key -> DartLocationTypeData.STOP
                else -> throw IllegalArgumentException("Unknown Dart LocationType: $locationType")
            }
        }

        override fun serialize(encoder: Encoder, value: DartLocationTypeData) {
            throw IllegalArgumentException("Not supported")
        }
    }
}
