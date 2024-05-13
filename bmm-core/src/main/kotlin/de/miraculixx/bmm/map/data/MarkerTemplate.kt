package de.miraculixx.bmm.map.data

import com.flowpowered.math.vector.Vector3d
import de.miraculixx.bmm.utils.Vec3dSerializer
import de.miraculixx.mcommons.serializer.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class MarkerTemplate(
    val name: String,
    val maxMarkerPerPlayer: Int,
    val neededPermission: String,
    val templateMarker: MutableMap<String, BMarker>,
    val playerMarkers: MutableMap<@Serializable(with = UUIDSerializer::class) UUID, MarkerTemplateEntry>
)

@Serializable
data class MarkerTemplateEntry(
    val templateName: String,
    val displayName: String,
    val position: @Serializable(with = Vec3dSerializer::class) Vector3d
)