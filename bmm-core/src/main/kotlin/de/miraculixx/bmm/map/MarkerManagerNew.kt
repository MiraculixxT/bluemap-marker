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

    val templateSets: MutableMap<String, MarkerTemplate> = mutableMapOf() // <templateName -> template>
    val blueMapMaps: MutableMap<String, MutableMap<String, BMarkerSet>> = mutableMapOf() // <mapID -> <setID -> set>>

    private val markerJson = Json {
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
        val invalidUUID = UUID(0,0)

        // Load normal sets
        folderSets.listFiles()?.forEach { file -> // List all world folders
            if (!file.isDirectory) return@forEach
            val mapID = file.name
            file.listFiles()?.forEach sets@{ setFile ->

                // Load set
                if (file.extension != "json") return@sets
                val setID = setFile.nameWithoutExtension
                val set = file.loadConfig(BMarkerSet(invalidUUID), markerJson).takeUnless { it.owner == invalidUUID }
                if (set == null) {
                    sendError("Marker set file for set '$setID' in map '$mapID' is invalid! Skipping it...")
                    return@forEach
                }
                set.load(blueMapAPI!!, setID, blueMapAPI!!.getMap(mapID).getOrNull() ?: return@sets)

            }
        }

        // Load template sets
        folderTemplateSets.listFiles()?.forEach { file ->
            if (file.extension != "json") return
            val template = file.loadConfig(MarkerTemplate("", markerSetID = ""), markerJson)
            if (template.name.isEmpty()) {
                sendError("Template file '${file.name}' is invalid! Skipping it...")
                return@forEach
            }
            template.load(blueMapAPI!!)
        }
    }

    fun save() {

    }



    fun sendError(info: String) {
        consoleAudience.sendMessage(prefix + cmp(info, cError))
    }

    fun noBlueMapAPI(): Boolean {
        sendError("BlueMapAPI is not available! Please contact support if this error persists.")
        return false
    }
}