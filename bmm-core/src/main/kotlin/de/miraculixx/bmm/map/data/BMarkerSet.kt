package de.miraculixx.bmm.map.data

import de.bluecolored.bluemap.api.BlueMapMap
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.miraculixx.bmm.map.MarkerBuilder
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.map.MarkerSetBuilder
import de.miraculixx.bmm.utils.enums.MarkerArg
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
data class BMarkerSet(
    val owner: @Contextual UUID,
    val attributes: MutableMap<MarkerArg, Box> = mutableMapOf(),
    var markers: MutableMap<String, BMarker> = mutableMapOf(),
) {
    @Transient
    var blueMapMarkerSet: MarkerSet? = null
        private set

    fun load(setID: String, map: BlueMapMap): MarkerSet {
        // Load set
        val set = MarkerSetBuilder.createSet(attributes)
        blueMapMarkerSet = set

        // Load markers
        markers.forEach { (markerID, marker) ->
            marker.load(markerID, set)
        }

        // Place set
        MarkerManager.blueMapMaps[map.id]?.put(setID, this) // only needed if the set is new, otherwise this will do nothing
        map.markerSets[setID] = set
        return set
    }

    fun getEditor(): MarkerSetBuilder? {
        return blueMapMarkerSet?.let { MarkerSetBuilder(attributes, it, true) }
    }

    fun addMarker(owner: UUID, builder: MarkerBuilder, markerID: String) {
        val bMarker = BMarker(owner, builder.getType(), builder.getArgs())
        markers[markerID] = bMarker
        blueMapMarkerSet?.let { bMarker.load(markerID, it) }
    }

    fun removeMarker(markerID: String): Boolean {
        blueMapMarkerSet?.markers?.remove(markerID)
        return markers.remove(markerID) != null
    }
}
