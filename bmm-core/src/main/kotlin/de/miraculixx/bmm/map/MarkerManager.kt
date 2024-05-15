package de.miraculixx.bmm.map

import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bmm.map.data.BMarkerSet
import de.miraculixx.bmm.map.data.MarkerTemplate
import de.miraculixx.bmm.utils.serializer.ColorSerializer
import de.miraculixx.bmm.utils.serializer.Vec2dSerializer
import de.miraculixx.bmm.utils.serializer.Vec2iSerializer
import de.miraculixx.bmm.utils.serializer.Vec3dSerializer
import de.miraculixx.bmm.utils.sourceFolder
import de.miraculixx.mcommons.extensions.loadConfig
import de.miraculixx.mcommons.extensions.saveConfig
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
object MarkerManager {
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
            contextual(Vec2dSerializer)
            contextual(ColorSerializer)
        }
    }
    var blueMapAPI: BlueMapAPI? = null


    fun load(api: BlueMapAPI) {
        blueMapAPI = api
        val invalidUUID = UUID(0,0)

        api.maps.forEach { map -> blueMapMaps[map.id] = mutableMapOf() }
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
                set.load(api, setID, api.getMap(mapID).getOrNull() ?: return@sets)

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
            template.load(api)
        }
    }

    fun save(api: BlueMapAPI) {
        // Prepare workspace
        folderSets.mkdirs()
        folderTemplateSets.mkdirs()

        // Save normal sets
        blueMapMaps.forEach { (mapID, sets) ->
            val worldName = api.getMap(mapID).getOrNull()?.name
            if (worldName == null) {
                sendError("Cannot find map '$mapID'! All sets inside this map will not save or update.")
                return@forEach
            }
            val worldFolder = File(folderSets, worldName)
            if (!worldFolder.exists()) worldFolder.mkdir()
            sets.forEach { (setID, set) ->
                val file = File(worldFolder, "$setID.json")
                file.saveConfig(set, markerJson)
            }
        }

        // Save template sets
        templateSets.forEach { (templateName, template) ->
            val file = File(folderTemplateSets, "$templateName.json")
            file.saveConfig(template, markerJson)
        }
    }

    /**
     * Save remove a set from the manager.
     * This also will remove all templates associated with it.
     * @return true if the set was removed, false if it was not found.
     */
    fun removeSet(mapID: String, setID: String): Boolean {
        templateSets.filter { it.value.markerSetID == setID }.forEach { (_, data) ->
            data.removeMap(mapID, blueMapAPI!!.getMap(mapID).getOrNull())
        }
        return blueMapMaps[mapID]?.remove(setID) != null
    }


    //
    // Error Handling Messages
    //
    fun sendError(info: String) {
        consoleAudience.sendMessage(prefix + cmp(info, cError))
    }
}