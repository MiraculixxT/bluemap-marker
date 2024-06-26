package de.miraculixx.bmm.map.data

import de.bluecolored.bluemap.api.markers.Marker
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.miraculixx.bmm.map.MarkerBuilder
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.enums.MarkerType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
data class BMarker(
    val owner: @Contextual UUID,
    val type: MarkerType = MarkerType.POI,
    val attributes: MutableMap<MarkerArg, Box> = mutableMapOf()
) {
    @Transient
    private var blueMapMarker: Marker? = null

    fun load(markerID: String, set: MarkerSet): Marker? {
        // Load marker
        blueMapMarker = MarkerBuilder.createMarker(attributes, type)

        // Place marker
        set.markers[markerID] = blueMapMarker
        return blueMapMarker
    }

    fun getEditor(): MarkerBuilder? {
        return blueMapMarker?.let { MarkerBuilder(type, attributes, it, true) }
    }
}
