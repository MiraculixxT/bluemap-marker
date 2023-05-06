package de.miraculixx.bmm.map

import com.google.gson.JsonSyntaxException
import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.gson.MarkerGson
import de.bluecolored.bluemap.api.markers.Marker
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.miraculixx.bmm.utils.message.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object MarkerManager {
    private val markerSets: MutableMap<String, MarkerSet> = mutableMapOf()

    /**
     * @param setID id_mapName
     * @param markerID Marker ID (unique)
     * @param marker Marker that should be added
     */
    fun addMarker(setID: String, marker: Marker, markerID: String): Boolean {
        val markerSet = markerSets[setID]
        if (markerSet == null) {
            consoleAudience.sendMessage(prefix + cmp("Failed to apply marker to set '$setID'! This marker-set does not exist", cError))
            return false
        }

        markerSet.markers[markerID] = marker
        return true
    }

    /**
     * @param setID id_mapName
     * @param markerID Marker ID (unique)
     * @return A matching loaded marker or null if no one could be found
     */
    fun getMarker(setID: String, markerID: String): Marker? {
        val markerSet = markerSets[setID]
        if (markerSet == null) {
            consoleAudience.sendMessage(prefix + cmp("Failed to get marker-set from ID '$setID'! This marker-set does not exist", cError))
            return null
        }
        return if (markerSet.markers.contains(markerID)) {
            markerSet.markers[markerID]
        } else {
            consoleAudience.sendMessage(prefix + cmp("Failed to get marker ($markerID) from set '$setID'"))
            null
        }
    }

    /**
     * @param setID id_mapName
     * @param markerID Maker ID (unique)
     * @return true if successfully removed
     */
    fun removeMarker(setID: String, markerID: String): Boolean {
        val markerSet = markerSets[setID] ?: return false
        return markerSet.markers.remove(markerID) != null
    }

    /**
     * @param setID id_mapName
     */
    fun getAllMarkers(setID: String): Map<String, Marker> {
        return markerSets[setID]?.markers ?: emptyMap()
    }

    /**
     * @param mapName World name
     * @return A list of all sets targeting the map with name [mapName]
     */
    fun getAllSetIDs(mapName: String): List<String> {
        return markerSets.keys.filter { it.contains("_$mapName") }.map { it.removeSuffix("_$mapName") }
    }

    /**
     * @return A list of all marker-sets
     */
    fun getAllSetIDs(): List<String> {
        return markerSets.keys.toList()
    }

    /**
     * @param rawID Raw set ID
     * @param mapName Target map name
     * @param set Set that should be added
     * @return Returns only true if the set was successfully added. Fails could appear through BlueMap is not loaded, world name does not exist or something else is broke
     */
    fun addSet(rawID: String, mapName: String, set: MarkerSet): Boolean {
        val setID = "${rawID}_$mapName"
        val api = getAPI() ?: return false
        val map = api.maps?.firstOrNull { it.name == mapName.replace('.', ' ') }
        if (map == null) {
            consoleAudience.sendMessage(prefix + cmp("Failed to access Map $mapName for set $rawID! Check if BlueMap loaded correctly and if you enter a valid map name!", cError))
            return false
        }

        markerSets[setID] = set
        map.markerSets[setID] = set
        return true
    }

    /**
     * @param rawID Raw set ID
     * @param mapName Target map name
     * @return Returns only true if the set was successfully removed. Fails could appear through BlueMap is not loaded, world name does not exist or set does not exist anymore
     */
    fun removeSet(rawID: String, mapName: String): Boolean {
        val setID = "${rawID}_$mapName"
        val api = getAPI() ?: return false
        val map = api.maps.firstOrNull { it.name == mapName.replace('.', ' ') } ?: return false

        map.markerSets.remove(setID)
        markerSets.remove(setID)
        return true
    }

    /**
     * @return Returns a list of all loaded BlueMap maps in string name formation
     */
    fun getAllMaps(): List<String> {
        val api = getAPI() ?: return emptyList()
        return api.maps.map { it.name.replace(' ', '.') }
    }

    fun loadAllMarker(api: BlueMapAPI, sourceFolder: File) {
        val gson = MarkerGson.INSTANCE
        val folder = prepareConfigFolder(sourceFolder)
        val file = File("${folder.path}/marker-sets.json")
        if (!file.exists()) file.writeText("[]")

        val setIDs = Json.decodeFromString<List<String>>(file.readText())
        setIDs.forEach { setID ->
            val data = setID.split("_", limit = 2)
            val mapName = data.getOrNull(1) ?: return@forEach
            val rawID = data.getOrNull(0) ?: return@forEach
            val markerFile = File("${folder.path}/${setID}.json")
            val set = if (markerFile.exists()) {
                consoleAudience.sendMessage(prefix + cmp("Found markers for marker-set '$mapName'/'$rawID' - Loading ${markerFile.length() / 1000.0} kb"))
                try {
                    gson.fromJson(markerFile.readText(), MarkerSet::class.java)
                } catch (e: JsonSyntaxException) {
                    consoleAudience.sendMessage(prefix + cmp("Marker file for set '$setID' is invalid! Skipping it...", cError))
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
            val map = api.maps.firstOrNull { it.name == mapName.replace('.', ' ') }
            if (map == null) {
                consoleAudience.sendMessage(
                    prefix + cmp(
                        "Cannot find map '$mapName' for marker-set '$rawID'! If you rename your map in BlueMap, edit the marker-set before!",
                        cError
                    )
                )
                return@forEach
            }
            map.markerSets[setID] = set
        }
    }

    fun saveAllMarker(sourceFolder: File) {
        if (markerSets.isEmpty()) return

        val folder = prepareConfigFolder(sourceFolder)
        val sets = File("${folder.path}/marker-sets.json")
        sets.writeText(json.encodeToString(markerSets.keys))
        markerSets.forEach { (id, set) ->
            val file = File("${folder.path}/$id.json")
            file.writeText(gson.toJson(set))
        }
        markerSets.clear()
    }

    private fun prepareConfigFolder(sourceFolder: File): File {
        if (!sourceFolder.exists()) sourceFolder.mkdir()
        val markerFolder = File("${sourceFolder.path}/marker")
        if (!markerFolder.exists()) markerFolder.mkdir()
        return markerFolder
    }

    private fun getAPI(): BlueMapAPI? {
        val instance = BlueMapAPI.getInstance()
        return if (!instance.isPresent) {
//            consoleAudience.sendMessage(prefix + cmp("Failed to access BlueMap API! Try /bluemap reload and look for errors or contact support", cError))
            null
        } else instance.get()
    }
}