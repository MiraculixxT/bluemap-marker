package de.miraculixx.bmm.map

import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bmm.map.data.BMarkerSet
import de.miraculixx.bmm.map.data.TemplateSet
import de.miraculixx.bmm.map.data.TemplateSetLoader
import de.miraculixx.bmm.utils.serializer.ColorSerializer
import de.miraculixx.bmm.utils.serializer.Vec2dSerializer
import de.miraculixx.bmm.utils.serializer.Vec2iSerializer
import de.miraculixx.bmm.utils.serializer.Vec3dSerializer
import de.miraculixx.bmm.utils.sourceFolder
import de.miraculixx.mcommons.debug
import de.miraculixx.mcommons.extensions.saveConfig
import de.miraculixx.mcommons.serializer.UUIDSerializer
import de.miraculixx.mcommons.text.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.io.File
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
    private val backupFolder = File(sourceFolder, "backup") // All old converted data will be saved here

    // Data Maps
    val templateSets: MutableMap<String, TemplateSet> = mutableMapOf() // <templateName -> template>
    val blueMapMaps: MutableMap<String, MutableMap<String, BMarkerSet>> = mutableMapOf() // <mapID -> <setID -> set>>

    // Marker (set) Builders
    val builder: MutableMap<String, MarkerBuilder> = mutableMapOf()
    val builderSet: MutableMap<String, MarkerSetBuilder> = mutableMapOf()

    // Data Loader
    var templateLoader: TemplateSetLoader? = null

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


    fun load(api: BlueMapAPI, isFabric: Boolean) {
        blueMapAPI = api

        api.maps.forEach { map -> blueMapMaps[map.id] = mutableMapOf() }
        if (debug) consoleAudience.sendMessage(prefix + cmp("Loading marker data for maps ${blueMapMaps.keys}..."))
        // Load normal sets
        folderSets.listFiles()?.forEach { file -> // List all world folders
            if (!file.isDirectory) return@forEach
            if (debug) consoleAudience.sendMessage(prefix + cmp(" - Load map '${file.name}'..."))
            val mapID = file.name
            val map = api.getMap(mapID).getOrNull()

            file.listFiles()?.forEach sets@{ setFile ->

                // Load set
                if (setFile.extension != "json") return@sets
                val setID = setFile.nameWithoutExtension
                if (map == null) { // Recover sets that were saved by name instead by ID
                    sendError("   - Cannot find map '$mapID'! Trying to fix '$setID'...")
                    recoverSetByName(mapID, setID, setFile, api)
                    return@sets
                }
                if (debug) consoleAudience.sendMessage(prefix + cmp("   - Load set '$setID'..."))
                val set = readSafe<BMarkerSet>(setFile) { sendError("Marker set file for set '$setID' in map '$mapID' is invalid ($it)! Skipping it...") }
                if (set == null) return@sets
                set.load(setID, map)
                if (debug) consoleAudience.sendMessage(prefix + cmp("   - Loaded set '$setID'!"))
            }
            if (map == null) file.deleteRecursively()
        }

        // Load template sets. Fabric already read them at mod init, because its commands have to exist
        // before the server starts, only their markers still need to be placed onto the maps.
        if (isFabric) loadTemplateMarkers(api) else loadTemplates(api)
    }

    fun loadTemplates(api: BlueMapAPI?) {
        // Load template sets
        if (debug) println("[BMM] Loading template data...")
        folderTemplateSets.listFiles()?.forEach { file ->
            if (file.extension != "json") return@forEach
            if (debug) println("[BMM]   - Load template '${file.nameWithoutExtension}'...")
            val warn: (String) -> Unit = { info ->
                if (api == null) println("[BMM-Warn] $info")
                else sendError(info)
            }
            val template = readSafe<TemplateSet>(file) { warn("Template file '${file.name}' is invalid ($it)! Skipping it...") }
            if (template == null || template.name.isEmpty()) {
                if (template != null) warn("Template file '${file.name}' has no name! Skipping it...")
                return@forEach
            }
            val known = templateSets[template.name]
            if (known != null) { // Already loaded (BlueMap reload) - only place it again, never register twice
                api?.let { known.load(it) }
                return@forEach
            }
            templateSets[template.name] = template
            api?.let { template.load(it) } ?: if (debug) println("[BMM]  - Loading pre api...") else Unit
            templateLoader?.loadTemplate(template)
            if (debug) println("[BMM]  - Loaded template '${template.name}'!")
        }
    }

    /**
     * Place all known template sets onto the BlueMap maps.
     * Used when the templates were already read from disk before the BlueMap API was available.
     */
    fun loadTemplateMarkers(api: BlueMapAPI) {
        templateSets.values.toList().forEach { it.load(api) }
    }

    /**
     * Marker sets owned by a template set. Those are fully managed by their template and must not be
     * touched by the normal marker commands.
     */
    fun isTemplateSet(setID: String) = templateSets.values.any { it.markerSetID == setID }

    /**
     * Look up a set for the normal marker commands. Template owned sets are treated as non existing.
     */
    fun getSet(mapID: String?, setID: String?): BMarkerSet? {
        if (mapID == null || setID == null || isTemplateSet(setID)) return null
        return blueMapMaps[mapID]?.get(setID)
    }

    /**
     * All sets of a map that the normal marker commands may use.
     */
    fun getSets(mapID: String?): Map<String, BMarkerSet> {
        return blueMapMaps[mapID]?.filterKeys { !isTemplateSet(it) } ?: emptyMap()
    }

    /**
     * Read a config file without ever destroying it.
     * A file that cannot be parsed is copied into the backup folder and skipped.
     */
    private inline fun <reified T> readSafe(file: File, onError: (String) -> Unit): T? {
        return runCatching { markerJson.decodeFromString<T>(file.readText()) }.getOrElse { exception ->
            runCatching { file.copyTo(File(backupFolder, "${file.parentFile.name}/${file.name}"), true) }
            onError(exception.message ?: exception.javaClass.simpleName)
            null
        }
    }

    /**
     * Write a single marker set to disk. Used to persist every change immediately.
     */
    fun saveSet(mapID: String, setID: String) {
        val set = blueMapMaps[mapID]?.get(setID) ?: return
        File(folderSets, mapID).mkdirs()
        File(folderSets, "$mapID/$setID.json").saveConfig(set, markerJson)
    }

    /**
     * Write a single template set to disk. Used to persist every change immediately.
     */
    fun saveTemplate(templateName: String) {
        val template = templateSets[templateName] ?: return
        folderTemplateSets.mkdirs()
        File(folderTemplateSets, "$templateName.json").saveConfig(template, markerJson)
    }

    fun deleteSetFile(mapID: String, setID: String) {
        File(folderSets, "$mapID/$setID.json").delete()
    }

    fun deleteTemplateFile(templateName: String) {
        File(folderTemplateSets, "$templateName.json").delete()
    }

    fun save(api: BlueMapAPI) {
        // Prepare workspace
        folderSets.mkdirs()
        folderTemplateSets.mkdirs()

        // Save normal sets
        blueMapMaps.forEach { (mapID, sets) ->
            val worldFolder = File(folderSets, mapID)
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
            data.removeMap(mapID, blueMapAPI?.getMap(mapID)?.getOrNull())
            saveTemplate(data.name)
        }
        blueMapAPI?.getMap(mapID)?.getOrNull()?.markerSets?.remove(setID)
        val removed = blueMapMaps[mapID]?.remove(setID) != null
        if (removed) deleteSetFile(mapID, setID)
        return removed
    }


    //
    // Error Handling Messages
    //
    fun sendError(info: String) {
        consoleAudience.sendMessage(prefix + cmp(info, cError))
    }

    /**
     * Tries to recover sets that were potentially saved by name instead by ID.
     */
    private fun recoverSetByName(mapName: String, setID: String, sourceFile: File, api: BlueMapAPI) {
        val map = api.maps.firstOrNull { it.name == mapName }
        if (map == null) {
            sendError("   - Failed to recover set '$setID' in '$mapName'! Delete to remove this message.")
            return
        }
        sourceFile.copyTo(File(backupFolder, "${sourceFile.parentFile.name}/${sourceFile.name}"), true) // Backup
        val targetFix = File(folderSets, "${map.id}/$setID.json")
        if (targetFix.exists()) {
            sendError("   - Set '$setID' already exists in '$mapName'! Moving invalid to backup...")
            sourceFile.delete()
            return
        }
        sourceFile.copyTo(targetFix)
        consoleAudience.sendMessage(prefix + cmp("   - Successfully recovered set '$setID' in '$mapName'! (Restart to finalize)"))
        sourceFile.delete()
    }
}