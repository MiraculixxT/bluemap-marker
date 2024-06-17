package de.miraculixx.bmm.utils.serializer

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector2i
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object Vec2dSerializer : KSerializer<Vector2d> {
    override val descriptor = PrimitiveSerialDescriptor("Vector2d", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Vector2d {
        return decoder.decodeString().split(",").let {
            Vector2d(it[0].toDouble(), it[1].toDouble())
        }
    }

    override fun serialize(encoder: Encoder, value: Vector2d) {
        encoder.encodeString("${value.x},${value.y}")
    }
}