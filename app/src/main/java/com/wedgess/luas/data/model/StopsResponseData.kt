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
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@XmlSerialName("stops", namespace = "", prefix = "")
data class StopsResponseData(
    @XmlElement(true) val line: List<LineData>
) {
    @Serializable
    @XmlSerialName("line", namespace = "", prefix = "")
    data class LineData(
        @Serializable(with = LineSerializer::class)
        @XmlSerialName("name") val name: LuasLineData,
        @XmlElement(true) val stop: List<StopData>
    ) {


        @Serializable
        @XmlSerialName("stop", namespace = "", prefix = "")
        data class StopData(
            @XmlSerialName("abrev") val abrev: String,
            @XmlSerialName("isParkRide") val isParkRide: Int,
            @XmlSerialName("isCycleRide") val isCycleRide: Int,
            @XmlSerialName("lat") val lat: Double,
            @XmlSerialName("long") val long: Double,
            @XmlSerialName("pronunciation") val pronunciation: String,
            @XmlValue val name: String
        )
    }
}

private object LineSerializer : KSerializer<LuasLineData> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(LuasLineData::class::simpleName.name, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LuasLineData {
        val lineName = decoder.decodeString().lowercase()

        return when {
            lineName.contains("red") -> LuasLineData.RED
            lineName.contains("green") -> LuasLineData.GREEN
            else -> throw IllegalArgumentException("Unknown luas line: $lineName")
        }
    }

    override fun serialize(encoder: Encoder, value: LuasLineData) {
        throw IllegalArgumentException("Not supported")
    }
}
