package de.miraculixx.bm_marker.map

import com.google.gson.JsonSyntaxException
import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.gson.MarkerGson
import de.bluecolored.bluemap.api.markers.Marker
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.miraculixx.bm_marker.PluginManager
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.axay.kspigot.extensions.bukkit.warn
import net.axay.kspigot.extensions.console
import net.axay.kspigot.extensions.worlds
import java.io.File

object MarkerManager {
    private val markerSets: MutableMap<String, MarkerSet> = mutableMapOf()

    /**
     * @param setID id_worldName
     * @param markerID Marker ID (unique)
     * @param marker Marker that should be added
     */
    fun addMarker(setID: String, marker: Marker, markerID: String) {
        val markerSet = markerSets[setID]

        if (markerSet == null) {
            console.warn("Failed to apply marker to set '$setID'! This marker-set does not exist")
            return
        }

        markerSet.markers[markerID] = marker
    }

    /**
     * @param setID id_worldName
     * @param markerID Maker ID (unique)
     * @return true if successfully removed
     */
    fun removeMarker(setID: String, markerID: String): Boolean {
        val markerSet = markerSets[setID] ?: return false
        return markerSet.markers.remove(markerID) != null
    }

    /**
     * @param setID id_worldName
     */
    fun getAllMarkers(setID: String): Map<String, Marker> {
        return markerSets[setID]?.markers ?: emptyMap()
    }

    /**
     * @param worldName World name
     * @return A list of all sets targeting the world with name [worldName]
     */
    fun getAllSetIDs(worldName: String): List<String> {
        return markerSets.keys.filter { it.contains("_$worldName") }.map { it.removeSuffix("_$worldName") }
    }

    /**
     * @return A list of all marker-sets
     */
    fun getAllSetIDs(): List<String> {
        return markerSets.keys.toList()
    }

    /**
     * @param rawID Raw set ID
     * @param worldName Target world name
     * @param set Set that should be added
     * @return Returns only true if the set was successfully added. Fails could appear through BlueMap is not loaded, world name does not exist or something else is broke
     */
    fun addSet(rawID: String, worldName: String, set: MarkerSet): Boolean {
        val setID = "${rawID}_$worldName"
        val world = worlds.firstOrNull { it.key.key == worldName } ?: return false
        val blueMapAPI = BlueMapAPI.getInstance()
        if (!blueMapAPI.isPresent) return false

        markerSets[setID] = set
        blueMapAPI.ifPresent {
            it.getWorld(world.uid).ifPresent { bWorld ->
                bWorld.maps.forEach { map ->
                    map.markerSets[setID] = set
                }
            }
        }
        return true
    }

    /**
     * @param rawID Raw set ID
     * @param worldName Target world name
     * @return Returns only true if the set was successfully removed. Fails could appear through BlueMap is not loaded, world name does not exist or set does not exist anymore
     */
    fun removeSet(rawID: String, worldName: String): Boolean {
        val setID = "${rawID}_$worldName"
        val world = worlds.firstOrNull { it.key.key == worldName } ?: return false
        val blueMapAPI = BlueMapAPI.getInstance()
        if (!blueMapAPI.isPresent) return false

        blueMapAPI.ifPresent {
            it.getWorld(world.uid).ifPresent { bWorld ->
                bWorld.maps.forEach { map ->
                    map.markerSets.remove(setID)
                }
            }
        }
        markerSets.remove(setID)
        return true
    }

    fun loadAllMarker(blueMapAPI: BlueMapAPI) {
        val gson = MarkerGson.INSTANCE
        val logger = PluginManager.logger
        val folder = prepareConfigFolder()
        val file = File("${folder.path}/marker-sets.json")
        if (!file.exists()) {
            file.mkdir()
            file.writeText("{}")
        }
        val setIDs = Json.decodeFromString<List<String>>(file.readText())
        setIDs.forEach { setID ->
            val data = setID.split('_')
            val worldName = data.getOrNull(1) ?: return@forEach
            val rawID = data.getOrNull(0) ?: return@forEach
            val markerFile = File("${folder.path}/${setID}.json")
            val set = if (markerFile.exists()) {
                logger.info("Found markers for marker-set '$worldName'/'$rawID' - Loading ${markerFile.length() / 1000.0} kb")
                try {
                    gson.fromJson(markerFile.readText(), MarkerSet::class.java)
                } catch (e: JsonSyntaxException) {
                    logger.warning("Marker file for set '$setID' is invalid! Skipping it...")
                    return@forEach
                }
            } else {
                MarkerSet.builder()
                    .defaultHidden(false)
                    .toggleable(true)
                    .label("Custom Marker")
                    .build()
            }

            markerSets[setID] = set
            val world = worlds.firstOrNull { it.key.key == worldName }
            if (world == null) {
                logger.warning("Cannot find world '$worldName' for marker-set '$rawID'! Please remove this set via /bmarker delete-set or manually if this is intended")
                return@forEach
            }
            blueMapAPI.getWorld(world.uid).ifPresent {
                it.maps.forEach { map ->
                    map.markerSets[setID] = set
                }
            }
        }
    }

    fun saveAllMarker() {
        val gson = MarkerGson.INSTANCE
        val folder = prepareConfigFolder()
        markerSets.forEach { (id, set) ->
            val file = File("${folder.path}/$id.json")
            file.writeText(gson.toJson(set))
        }
        markerSets.clear()
    }

    private fun prepareConfigFolder(): File {
        val sourceFolder = PluginManager.dataFolder
        if (!sourceFolder.exists()) sourceFolder.mkdir()
        val markerFolder = File("${sourceFolder.path}/marker")
        if (!markerFolder.exists()) markerFolder.mkdir()
        return markerFolder
    }
}