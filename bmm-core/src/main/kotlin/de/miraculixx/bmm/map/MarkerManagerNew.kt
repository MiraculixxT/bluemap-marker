package de.miraculixx.bmm.map

import de.miraculixx.bmm.map.data.BMarkerSet
import de.miraculixx.bmm.map.data.MarkerTemplate
import de.miraculixx.bmm.utils.sourceFolder
import de.miraculixx.mcommons.extensions.loadConfig
import de.miraculixx.mcommons.text.*
import java.io.File
import java.util.UUID


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
    private val fileTemplateSet = File(sourceFolder, "templates/sets.json")
    private val folderSets = File(sourceFolder, "data") // data/<world>/<set-id>.json

    private val templateSets: Map<String, MarkerTemplate> = fileTemplateSet.loadConfig(emptyMap()) // <name, data>
    private val markerSets: MutableMap<String, BMarkerSet> = mutableMapOf()


    fun load() {
        val invalidUUID = UUID(0,0)
        folderSets.listFiles()?.forEach { file ->
            if (!file.isDirectory) return@forEach
            val mapID = file.name
            file.listFiles()?.forEach sets@{ setFile ->

                if (file.extension != "json") return@sets
                val setID = setFile.nameWithoutExtension
                val set = file.loadConfig(BMarkerSet(invalidUUID)).takeUnless { it.owner == invalidUUID }
                if (set == null) {
                    consoleAudience.sendMessage(prefix + cmp("Marker set file for set '$setID' in map '$mapID' is invalid! Skipping it...", cError))
                    return@forEach
                }

            }
        }
    }


}