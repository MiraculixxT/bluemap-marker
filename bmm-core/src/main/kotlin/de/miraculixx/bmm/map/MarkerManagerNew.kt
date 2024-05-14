package de.miraculixx.bmm.map

import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.miraculixx.bmm.map.data.BMarkerSet
import de.miraculixx.bmm.map.data.MarkerTemplate
import de.miraculixx.bmm.utils.serializer.ColorSerializer
import de.miraculixx.bmm.utils.serializer.Vec2iSerializer
import de.miraculixx.bmm.utils.serializer.Vec3dSerializer
import de.miraculixx.bmm.utils.sourceFolder
import de.miraculixx.mcommons.extensions.loadConfig
import de.miraculixx.mcommons.serializer.UUIDSerializer
import de.miraculixx.mcommons.text.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.io.File
import java.util.UUID
import kotlin.jvm.optionals.getOrNull


/**
 * Marker Concept
 *
 * - Creating Sets like normal based on bluemap maps
 * - Creating Markers like normal based on sets
 *
 * - Creating Template Sets based on bluemap maps.
 * Those sets will have a unique name that will be the new command name.
 * Template sets can be populated by template markers.
 * Players then can enter `/<set-template> place <marker-template> <name>` to place a marker.
 */
object MarkerManagerNew {
    // Storage Files
    private val folderTemplateSets = File(sourceFolder, "templates")
    private val folderSets = File(sourceFolder, "data") // data/<world>/<set-id>.json

    val templateSets: MutableMap<String, MarkerTemplate> = mutableMapOf() // <name, data>
    val markerSets: MutableMap<String, BMarkerSet> = mutableMapOf() // <id, data>

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        serializersModule = SerializersModule {
            contextual(UUIDSerializer)
            contextual(Vec3dSerializer)
            contextual(Vec2iSerializer)
            contextual(ColorSerializer)
        }
    }
    private var blueMapAPI: BlueMapAPI? = null


    fun load() {
        blueMapAPI = BlueMapAPI.getInstance().getOrNull()
        if (blueMapAPI == null) {
            sendError("BlueMapAPI is not available! Disabling MarkerManager...")
            return
        }

        // Load templates
        folderTemplateSets.listFiles()?.forEach { file ->

            // Parsing template file
            if (file.extension != "json") return@forEach
            val template = file.loadConfig(MarkerTemplate(""), json).takeUnless { it.name.isEmpty() }
            if (template == null) {
                sendError("Template file '${file.name}' is invalid! Skipping it...")
                return@forEach
            }
            templateSets[template.name] = template


        }



        val invalidUUID = UUID(0,0)
        folderSets.listFiles()?.forEach { file ->
            if (!file.isDirectory) return@forEach
            val mapID = file.name
            file.listFiles()?.forEach sets@{ setFile ->

                if (file.extension != "json") return@sets
                val setID = setFile.nameWithoutExtension
                val set = file.loadConfig(BMarkerSet(invalidUUID)).takeUnless { it.owner == invalidUUID }
                if (set == null) {
                    sendError("Marker set file for set '$setID' in map '$mapID' is invalid! Skipping it...")
                    return@forEach
                }

                // Load set markers

            }
        }
    }


    /**
     * Add a set to the given BlueMap map
     * @param map id of the map
     */
    fun addMarkerSet(mapID: String, markerSet: MarkerSet, markerSetID: String): Boolean {
        val api = blueMapAPI ?: return noBlueMapAPI()
        val map = api.getMap(mapID).getOrNull() ?: return false
        map.markerSets[markerSetID] = markerSet
        markerSets
    }

    fun sendError(info: String) {
        consoleAudience.sendMessage(prefix + cmp(info, cError))
    }

    private fun noBlueMapAPI(): Boolean {
        sendError("BlueMapAPI is not available! Please contact support if this error persists.")
        return false
    }
}