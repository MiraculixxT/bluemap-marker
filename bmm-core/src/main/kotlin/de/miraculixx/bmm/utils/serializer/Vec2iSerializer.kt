package de.miraculixx.bmm.utils.serializer

import com.flowpowered.math.vector.Vector2i
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object Vec2iSerializer : KSerializer<Vector2i> {
    override val descriptor = PrimitiveSerialDescriptor("Vector2i", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Vector2i {
        return decoder.decodeString().split(",").let {
            Vector2i(it[0].toInt(), it[1].toInt())
        }
    }

    override fun serialize(encoder: Encoder, value: Vector2i) {
        encoder.encodeString("${value.x},${value.y}")
    }
}