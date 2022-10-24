package de.miraculixx.bm_marker.map

import com.google.gson.JsonSyntaxException
import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.gson.MarkerGson
import de.bluecolored.bluemap.api.markers.Marker
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.miraculixx.bm_marker.PluginManager
import net.axay.kspigot.extensions.bukkit.warn
import net.axay.kspigot.extensions.console
import net.axay.kspigot.extensions.worlds
import java.io.File

object MarkerManager {
    private val markerSets: MutableMap<String, MarkerSet> = mutableMapOf()

    fun addMarker(worldName: String, marker: Marker, markerID: String) {
        val markerSet = markerSets["CUSTOM_MARKER_$worldName"]

        if (markerSet == null) {
            console.warn("Failed to apply marker to $worldName! Please reload BlueMap after creating new worlds")
            return
        }

        markerSet.markers[markerID] = marker
    }

    fun removeMarker(worldName: String, markerID: String): Boolean {
        val markerSet = markerSets["CUSTOM_MARKER_$worldName"] ?: return false
        return markerSet.markers.remove(markerID) != null
    }

    fun getAllMarkers(worldName: String): Map<String, Marker> {
        return markerSets["CUSTOM_MARKER_$worldName"]?.markers ?: emptyMap()
    }

    fun loadAllMarker(blueMapAPI: BlueMapAPI) {
        val gson = MarkerGson.INSTANCE
        val logger = PluginManager.logger
        val folder = prepareConfigFolder()
        worlds.forEach { world ->
            val worldName = world.key.key
            println(worldName)
            val markerFile = File("${folder.path}/${worldName}.json")
            val set = if (markerFile.exists()) {
                logger.info("Found markers for world '$worldName' - Loading ${markerFile.length() / 1000.0}kb")
                try {
                    gson.fromJson(markerFile.readText(), MarkerSet::class.java)
                } catch (e: JsonSyntaxException) {
                    logger.warning("Marker file for world $worldName is invalid! Skipping it...")
                    return@forEach
                }
            } else {
                MarkerSet.builder()
                    .defaultHidden(false)
                    .toggleable(true)
                    .label("Custom Marker")
                    .build()
            }

            markerSets["CUSTOM_MARKER_$worldName"] = set
            blueMapAPI.getWorld(world.uid).ifPresent {
                it.maps.forEach { map ->
                    map.markerSets["CUSTOM_MARKER_${world.name}"] = set
                }
            }
        }
    }

    fun saveAllMarker() {
        val gson = MarkerGson.INSTANCE
        val folder = prepareConfigFolder()
        markerSets.forEach { (id, set) ->
            val file = File("${folder.path}/${id.removePrefix("CUSTOM_MARKER_")}.json")
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