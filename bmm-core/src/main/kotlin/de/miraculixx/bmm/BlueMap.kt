package de.miraculixx.bmm

import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.Settings
import de.miraculixx.bmm.utils.message.*
import de.miraculixx.bmm.utils.settings
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.function.Consumer

var localization: Localization? = null

class BlueMap(sourceFolder: File, version: Int) {
    private val configFile = File(sourceFolder, "settings.json")

    private val onEnable = Consumer<BlueMapAPI> {
        consoleAudience.sendMessage(prefix + cmp("Connect to BlueMap API..."))
        settings.apply {
            val s = json.decodeFromString<Settings>(configFile.takeIf { f -> f.exists() }?.readText()?.ifBlank { "{}" } ?: "{}")
            language = s.language
        }
        val languages = listOf("en_US", "de_DE").map { key -> key to javaClass.getResourceAsStream("/language/$key.yml") }
        localization = Localization(File(sourceFolder, "language"), settings.language, languages, prefix)
        MarkerManager.loadAllMarker(it, sourceFolder)
        consoleAudience.sendMessage(prefix + cmp("Successfully enabled Marker Command addition!"))
    }

    private val onDisable = Consumer<BlueMapAPI> {
        consoleAudience.sendMessage(prefix + cmp("Disconnecting from BlueMap API..."))
        MarkerManager.saveAllMarker(sourceFolder)
        configFile.writeText(json.encodeToString(settings))
        consoleAudience.sendMessage(prefix + cmp("Successfully saved all data. Waiting for BlueMap to reload..."))
    }

    fun disable() {
        BlueMapAPI.unregisterListener(onDisable)
        BlueMapAPI.unregisterListener(onEnable)
    }

    init {
        BlueMapAPI.onEnable(onEnable)
        BlueMapAPI.onDisable(onDisable)
    }
}