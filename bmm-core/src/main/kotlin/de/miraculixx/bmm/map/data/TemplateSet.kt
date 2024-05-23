package de.miraculixx.bmm.map.data

import com.flowpowered.math.vector.Vector3d
import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.BlueMapMap
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.miraculixx.bmm.map.MarkerBuilder
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.serializer.Vec3dSerializer
import de.miraculixx.bmm.utils.settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*
import kotlin.jvm.optionals.getOrNull

/**
 * @param name name of the template and command -> `/<name> <subcommand>`
 * @param maxMarkerPerPlayer -1 means unlimited
 * @param neededPermission null means no permission needed to use command
 * @param maps all maps where this template is available. Markers will only be placed in maps associated with the world
 */
@Serializable
data class TemplateSet(
    val name: String,
    var maxMarkerPerPlayer: Int = settings.maxUserMarker,
    val needPermission: Boolean = false,
    val maps: MutableSet<String> = mutableSetOf(),
    val markerSetID: String = "template_$name",
    val templateSet: MutableMap<MarkerArg, Box> = mutableMapOf(MarkerArg.LABEL to Box.BoxString(name)),
    val templateMarker: MutableMap<String, BMarker> = mutableMapOf(),
    val playerMarkers: MutableMap<String, MarkerTemplateEntry> = mutableMapOf() // <markerID, data>
) {
    @Transient
    val blueMapSets: MutableMap<String, MarkerSet> = mutableMapOf() // <mapID, set>

    fun load(api: BlueMapAPI) {
        // Load template set in all maps
        maps.forEach { mapID ->
            api.getMap(mapID).getOrNull()?.let { map -> addMap(mapID, map) }
        }

        // Load player markers
        playerMarkers.forEach playerMarker@{ (_, data) ->
            val templateMarker = templateMarker[data.templateName]
            if (templateMarker == null) {
                MarkerManager.sendError("Template marker '${data.id}' in template '${name}' is invalid! Skipping it...")
                return@playerMarker
            }
            placeMarker(data, templateMarker)
        }

        MarkerManager.templateSets[name] = this // only needed if the set is new, otherwise this will do nothing
    }

    fun placeMarker(entry: MarkerTemplateEntry, templateMarker: BMarker) {
        val markerArgs = templateMarker.attributes.toMutableMap().apply {
            this[MarkerArg.LABEL]?.let { this[MarkerArg.LABEL] = Box.BoxString(it.getString()?.replace("%NAME%", entry.playerName) ?: "Unknown") }
            this[MarkerArg.DETAIL]?.let { this[MarkerArg.DETAIL] = Box.BoxString(it.getString()?.replace("%NAME%", entry.playerName) ?: "Unknown") }
            this[MarkerArg.POSITION] = Box.BoxVector3d(entry.position)
        }
        val finalMarker = MarkerBuilder.createMarker(markerArgs, templateMarker.type)

        // Place marker
        entry.placedMaps.forEach { mapID ->
            blueMapSets[mapID]?.markers?.put(entry.id, finalMarker)
        }
    }

    fun unplaceMarker(entry: MarkerTemplateEntry) {
        entry.placedMaps.forEach { map ->
            blueMapSets[map]?.remove(entry.id)
        }
    }

    fun addMap(mapID: String, map: BlueMapMap) {
        maps.add(mapID)
        // Get already loaded set or copy template-set and load it
        blueMapSets[mapID] = MarkerManager.blueMapMaps[mapID]?.get(mapID)?.blueMapMarkerSet
            ?: BMarkerSet(UUID(0, 0), templateSet).load(markerSetID, map)
    }

    fun removeMap(mapID: String, map: BlueMapMap?) {
        map?.markerSets?.remove(markerSetID)
        blueMapSets.remove(mapID)
        maps.remove(mapID)
        buildSet {
            playerMarkers.forEach { (_, data) ->
                if (data.placedMaps.remove(mapID) && data.placedMaps.isEmpty()) add(data.id)
            }
        }.forEach { playerMarkers.remove(it) } // Remove entries without maps
    }

    fun remove() {
        MarkerManager.templateSets.remove(name)
        maps.forEach { map ->
            MarkerManager.blueMapMaps[map]?.remove(markerSetID)
        }
    }
}

@Serializable
data class MarkerTemplateEntry(
    val templateName: String,
    val playerName: String,
    val id: String,
    val position: @Serializable(with = Vec3dSerializer::class) Vector3d,
    val placedMaps: MutableSet<String> = mutableSetOf()
)

interface TemplateSetLoader {
    fun loadTemplate(templateSet: TemplateSet)
}
