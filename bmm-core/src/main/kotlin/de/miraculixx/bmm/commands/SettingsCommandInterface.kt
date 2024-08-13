package de.miraculixx.bmm.commands

import de.bluecolored.bluemap.api.BlueMapMap
import de.bluecolored.bluemap.api.gson.MarkerGson
import de.bluecolored.bluemap.api.markers.*
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.map.data.BMarker
import de.miraculixx.bmm.map.data.BMarkerSet
import de.miraculixx.bmm.map.data.Box
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.enums.MarkerType
import de.miraculixx.bmm.utils.locale
import de.miraculixx.mcommons.extensions.enumOf
import de.miraculixx.mcommons.text.*
import net.kyori.adventure.audience.Audience
import java.io.File
import java.util.*
import kotlin.jvm.optionals.getOrNull

interface SettingsCommandInterface {

    fun sendCurrentInfo(sender: Audience, value: String) {
        sender.sendMessage(prefix + locale.msg("command.settings.current", listOf(value)))
    }

    fun sendChangedInfo(sender: Audience, value: String) {
        sender.sendMessage(prefix + locale.msg("command.settings.changed", listOf(value)))
    }

    fun convertOldMarkers(sender: Audience, configFolder: File) {
        sender.sendMessage(prefix + cmp("Converting markers from version prior 2.0..."))
        val oldFolder = File(configFolder, "BlueMap-Marker/marker").takeIf { it.exists() } ?: File(configFolder, "bm-marker/marker")
        if (!oldFolder.exists()) {
            sender.sendMessage(prefix + cmp("No old markers found!", cError))
            return
        }
        val api = MarkerManager.blueMapAPI
        if (api == null) {
            sender.sendMessage(prefix + cmp("The converter only works after BlueMap loads!", cError))
            return
        }
        val gson = MarkerGson.INSTANCE

        oldFolder.listFiles()?.forEach { file ->
            if (file.extension != "json") return@forEach
            val name = file.nameWithoutExtension
            if (name == "marker-sets") return@forEach
            sender.sendMessage(prefix + cmp("Starting conversion of '$name'..."))
            val split = name.split('_', limit = 2)
            val setID = split.first()
            val mapName = split.getOrNull(1) ?: return@forEach // Invalid file
            val map = api.maps.firstOrNull { it.name == mapName || it.id == mapName }
            if (map == null) {
                sender.sendMessage(prefix + cmp(" - Map '$mapName' not found! Can not convert the set '$setID'", cError))
                return@forEach
            }

            val set = kotlin.runCatching {
                gson.fromJson(file.readText(), MarkerSet::class.java)
            }.onFailure {
                sender.sendMessage(prefix + cmp(" - Set '$setID' has an invalid json structure! Skipping it...", cError))
                sender.sendMessage(prefix + cmp(" - Error: ${it.message}", cError))
            }.getOrNull() ?: return

            integrateSet(set, map, setID)
            sender.sendMessage(prefix + cmp(" - Set '$setID' converted successfully! (${set.markers.size} markers)"))
        }
        sender.sendMessage(prefix + cmp("Conversion finished!", cSuccess))
    }

    fun convertIntegratedMarkers(sender: Audience, configFolder: File, mapID: String) {
        val mapFile = File(configFolder, "BlueMap/maps/$mapID.conf")
        if (!mapFile.exists()) {
            sender.sendMessage(prefix + cmp("BlueMap file for '$mapID' not found! ", cError) + cmp("(${mapFile.path})"))
            return
        }
        val api = MarkerManager.blueMapAPI
        if (api == null) {
            sender.sendMessage(prefix + cmp("The converter only works after BlueMap loads!", cError))
            return
        }
        val map = api.getMap(mapID).getOrNull()
        if (map == null) {
            sender.sendMessage(prefix + cmp("Map '$mapID' not found! Can not convert the markers!", cError))
            return
        }

        // Config Hackery
        val lines = mapFile.readLines().toMutableList()
        var fullMarkerContent = ""
        var keywordFound = false
        lines.indices.forEach { i ->
            if (lines[i].contains("marker-sets: {")) keywordFound = true
            if (keywordFound) {
                val content = lines[i]
                lines[i] = "#$content"
                fullMarkerContent += content
            }
        }
        mapFile.writeText(lines.joinToString("\n"))

        // Loading actual content
        map.markerSets.forEach { (setID, set) ->
            if (MarkerManager.blueMapMaps[mapID]?.containsKey(setID) == true) return@forEach // BMM marker set
            if (!fullMarkerContent.contains("$mapID:")) return@forEach // Very soft check if the set is in the file to avoid another addon set conversion
            sender.sendMessage(prefix + cmp("Starting conversion of '$setID'..."))
            integrateSet(set, map, setID)
            sender.sendMessage(prefix + cmp(" - Set '$setID' converted successfully! (${set.markers.size} markers)", cSuccess))
        }
        sender.sendMessage(prefix + cmp("Conversion finished! Reloading BlueMap...", cSuccess))
    }

    private fun integrateSet(set: MarkerSet, map: BlueMapMap, setID: String) {
        // Load set
        val bSet = BMarkerSet(UUID(0, 0), set.getArgs(map.id, setID))
        val finalSet = bSet.load(setID, map)

        // Load Markers
        set.markers.forEach { (id, marker) ->
            val bMarker = BMarker(UUID(0, 0), enumOf<MarkerType>(marker.type.uppercase()) ?: MarkerType.POI, marker.getArgs(id, map.id, setID))
            bSet.markers[id] = bMarker
            bMarker.load(id, finalSet)
        }
    }

    private fun MarkerSet.getArgs(mapID: String, setID: String): MutableMap<MarkerArg, Box> {
        return buildMap {
            put(MarkerArg.ID, Box.BoxString(setID))
            put(MarkerArg.MAP, Box.BoxString(mapID))
            put(MarkerArg.MARKER_SET, Box.BoxString(setID))
            put(MarkerArg.LABEL, Box.BoxString(label))
            put(MarkerArg.TOGGLEABLE, Box.BoxBoolean(isToggleable))
            put(MarkerArg.DEFAULT_HIDDEN, Box.BoxBoolean(isDefaultHidden))
            put(MarkerArg.LISTING_POSITION, Box.BoxInt(sorting))
        }.toMutableMap()
    }

    private fun Marker.getArgs(id: String, mapID: String, markerSetID: String): MutableMap<MarkerArg, Box> {
        return buildMap {
            put(MarkerArg.ID, Box.BoxString(id))
            put(MarkerArg.MAP, Box.BoxString(mapID))
            put(MarkerArg.MARKER_SET, Box.BoxString(markerSetID))
            put(MarkerArg.LABEL, Box.BoxString(label))
            put(MarkerArg.POSITION, Box.BoxVector3d(position))
            put(MarkerArg.LISTED, Box.BoxBoolean(isListed))
            put(MarkerArg.LISTING_POSITION, Box.BoxInt(sorting))

            if (this@getArgs is DetailMarker) put(MarkerArg.DETAIL, Box.BoxString(detail))
            if (this@getArgs is DistanceRangedMarker) {
                put(MarkerArg.MAX_DISTANCE, Box.BoxDouble(maxDistance))
                put(MarkerArg.MIN_DISTANCE, Box.BoxDouble(minDistance))
            }
            if (this@getArgs is ObjectMarker) {
                link.getOrNull()?.let { put(MarkerArg.LINK, Box.BoxString(it)) }
                put(MarkerArg.NEW_TAB, Box.BoxBoolean(isNewTab))
            }
            if (this@getArgs is POIMarker) {
                put(MarkerArg.ICON, Box.BoxString(iconAddress))
                put(MarkerArg.ANCHOR, Box.BoxVector2i(anchor))
            }
            if (this@getArgs is LineMarker) {
                put(MarkerArg.ADD_POSITION, Box.BoxVector3dList(line.points.toMutableList()))
                put(MarkerArg.DEPTH_TEST, Box.BoxBoolean(isDepthTestEnabled))
                put(MarkerArg.LINE_WIDTH, Box.BoxInt(lineWidth))
                put(MarkerArg.LINE_COLOR, Box.BoxColor(lineColor))
            }
            if (this@getArgs is ShapeMarker) {
                put(MarkerArg.ADD_EDGE, Box.BoxVector2dList(shape.points.toMutableList()))
                put(MarkerArg.HEIGHT, Box.BoxFloat(shapeY))
                put(MarkerArg.DEPTH_TEST, Box.BoxBoolean(isDepthTestEnabled))
                put(MarkerArg.LINE_WIDTH, Box.BoxInt(lineWidth))
                put(MarkerArg.LINE_COLOR, Box.BoxColor(lineColor))
                put(MarkerArg.FILL_COLOR, Box.BoxColor(fillColor))
            }
            if (this@getArgs is ExtrudeMarker) {
                put(MarkerArg.ADD_EDGE, Box.BoxVector2dList(shape.points.toMutableList()))
                put(MarkerArg.HEIGHT, Box.BoxFloat(shapeMinY))
                put(MarkerArg.MAX_HEIGHT, Box.BoxFloat(shapeMaxY))

                put(MarkerArg.DEPTH_TEST, Box.BoxBoolean(isDepthTestEnabled))
                put(MarkerArg.LINE_WIDTH, Box.BoxInt(lineWidth))
                put(MarkerArg.LINE_COLOR, Box.BoxColor(lineColor))
                put(MarkerArg.FILL_COLOR, Box.BoxColor(fillColor))
            }
        }.toMutableMap()
    }
}