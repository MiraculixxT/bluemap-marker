package de.miraculixx.bmm.utils.serializer

import com.flowpowered.math.vector.Vector3d
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object Vec3dSerializer: KSerializer<Vector3d> {
    override val descriptor = PrimitiveSerialDescriptor("Vector3d", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Vector3d {
        return decoder.decodeString().split(",").let {
            Vector3d(it[0].toDouble(), it[1].toDouble(), it[2].toDouble())
        }
    }

    override fun serialize(encoder: Encoder, value: Vector3d) {
        encoder.encodeString("${value.x},${value.y},${value.z}")
    }
}