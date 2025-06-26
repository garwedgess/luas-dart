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

@Serializable
@XmlSerialName("stopInfo", namespace = "", prefix = "")
data class LuasStopForcastResponseData(
    @XmlSerialName("created") val created: String,
    @XmlSerialName("stop") val stop: String,
    @XmlSerialName("stopAbv") val stopAbv: String,
    @XmlElement val message: String,
    @XmlSerialName("direction")
    @XmlElement(true) val direction: List<DirectionData>
) {
    @Serializable
    data class DirectionData(
        @Serializable(with = DirectionKeyDataSerializer::class)
        @XmlSerialName("name") val name: LuasDirectionKeyData,
        @XmlSerialName("tram")
        @XmlElement(true) val tram: List<TramData>
    ) {
        @Serializable
        data class TramData(
            @XmlSerialName("dueMins") val dueMins: String,
            @XmlSerialName("destination") val destination: String
        )
    }
}

private object DirectionKeyDataSerializer : KSerializer<LuasDirectionKeyData> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(LuasDirectionKeyData::class::simpleName.name, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LuasDirectionKeyData {
        val directionKey = decoder.decodeString().lowercase()

        return LuasDirectionKeyData[directionKey]
    }

    override fun serialize(encoder: Encoder, value: LuasDirectionKeyData) {
        throw IllegalArgumentException("Not supported")
    }
}
