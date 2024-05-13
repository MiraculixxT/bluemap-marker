package de.miraculixx.bmm.map.data

import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.mcommons.serializer.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class BMarker(
    val owner: @Serializable(with = UUIDSerializer::class) UUID,
    val template: String? = null,
    val attributes: Set<MarkerArg> = emptySet()
)

@Serializable
data class BMarkerSet(
    val owner: @Serializable(with = UUIDSerializer::class) UUID,
    val template: String? = null,
    val attributes: Set<MarkerArg> = emptySet(),
    var markers: MutableMap<String, BMarker> = mutableMapOf()
)
