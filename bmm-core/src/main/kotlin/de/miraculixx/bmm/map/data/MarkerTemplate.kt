package de.miraculixx.bmm.map.data

import com.flowpowered.math.vector.Vector3d
import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.miraculixx.bmm.map.MarkerBuilder
import de.miraculixx.bmm.map.MarkerManagerNew
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.serializer.Vec3dSerializer
import de.miraculixx.mcommons.serializer.UUIDSerializer
import kotlinx.serialization.Contextual
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
data class MarkerTemplate(
    val name: String,
    val maxMarkerPerPlayer: Int = -1,
    val neededPermission: String? = null,
    val maps: MutableSet<String> = mutableSetOf(),
    val markerSetID: String,
    val templateSet: BMarkerSet = BMarkerSet(UUID(0, 0)),
    val templateMarker: MutableMap<String, BMarker> = mutableMapOf(),
    val playerMarkers: MutableMap<@Contextual UUID, MarkerTemplateEntry> = mutableMapOf()
) {
    @Transient
    private val blueMapSets: MutableMap<String, MarkerSet> = mutableMapOf() // <mapID, set>

    fun load(api: BlueMapAPI) {
        // Load template set in all maps
        blueMapSets.putAll(
            maps.mapNotNull { mapID ->
                api.getMap(mapID).getOrNull()?.let { map ->
                    templateSet.copy().load(api, markerSetID, map) // Copy template-set and load it
                }?.let { mapID to it }
            }.toMap()
        )

        // Load player markers
        playerMarkers.forEach playerMarker@{ (_, data) ->
            val templateMarker = templateMarker[data.templateName]
            if (templateMarker == null) {
                MarkerManagerNew.sendError("Template marker '${data.displayName}' in template '${name}' is invalid! Skipping it...")
                return@playerMarker
            }
            val markerArgs = templateMarker.attributes.toMutableMap().apply {
                this[MarkerArg.LABEL] = Box(data.displayName.replace("%PLAYER%", data.playerName))
                this[MarkerArg.DETAIL]?.let { this[MarkerArg.DETAIL] = Box(it.getString().replace("%PLAYER%", data.playerName)) }
            }
            val finalMarker = MarkerBuilder.ofArguments(markerArgs, templateMarker.type)

            // Place marker
            data.placedMaps.forEach { mapID ->
                blueMapSets[mapID]?.markers?.put(data.id, finalMarker)
            }
        }

        MarkerManagerNew.templateSets[name] = this // only needed if the set is new, otherwise this will do nothing
    }
}

@Serializable
data class MarkerTemplateEntry(
    val templateName: String,
    val displayName: String,
    val playerName: String,
    val id: String,
    val position: @Serializable(with = Vec3dSerializer::class) Vector3d,
    val placedMaps: Set<String> = emptySet()
)
