package de.miraculixx.bmm.map

import de.miraculixx.bmm.map.data.BMarkerSet
import de.miraculixx.bmm.map.data.MarkerTemplate
import de.miraculixx.bmm.utils.sourceFolder
import de.miraculixx.mcommons.extensions.loadConfig
import de.miraculixx.mcommons.text.*
import java.io.File
import java.util.UUID

object MarkerManagerNew {
    // Storage Files
    private val fileTemplateSet = File(sourceFolder, "templates/sets.json")
    private val fileTemplateMarker = File(sourceFolder, "templates/markers.json")
    private val folderSets = File(sourceFolder, "data") // data/<world>/<set-id>.json

    private val templatesSet: Map<String, MarkerTemplate> = fileTemplateSet.loadConfig(emptyMap()) // <name, data>
    private val templateMarkers: Map<String, MarkerTemplate> = fileTemplateMarker.loadConfig(emptyMap()) // <name, data>

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

                s

            }
        }
    }


}