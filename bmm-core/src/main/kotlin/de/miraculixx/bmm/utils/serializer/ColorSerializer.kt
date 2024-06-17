package de.miraculixx.bmm.utils.serializer

import de.bluecolored.bluemap.api.math.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ColorSerializer : KSerializer<Color> {
    override val descriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Color {
        return decoder.decodeString().split(",").let {
            Color(it[0].toInt(), it[1].toInt(), it[2].toInt(), it[3].toFloat())
        }
    }

    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeString("${value.red},${value.green},${value.blue},${value.alpha}")
    }
}