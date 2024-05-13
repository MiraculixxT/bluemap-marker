package de.miraculixx.bmm

import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.Settings
import de.miraculixx.bmm.utils.settings
import de.miraculixx.mcommons.serializer.jsonPretty
import de.miraculixx.mcommons.text.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.Locale
import java.util.function.Consumer

var localization: Localization? = null

class BlueMap(sourceFolder: File, version: Int) {
    private val configFile = File(sourceFolder, "settings.json")

    private val onEnable = Consumer<BlueMapAPI> {
        consoleAudience.sendMessage(prefix + cmp("Connect to BlueMap API..."))
        settings.apply {
            val s = jsonPretty.decodeFromString<Settings>(configFile.takeIf { f -> f.exists() }?.readText()?.ifBlank { "{}" } ?: "{}")
            language = s.language
        }
        val languages = listOf(Locale.ENGLISH, Locale.GERMAN).map { key -> key to javaClass.getResourceAsStream("/language/$key.yml") }
        localization = Localization(File(sourceFolder, "language"), settings.language, languages)
        MarkerManager.loadAllMarker(it, sourceFolder)
        consoleAudience.sendMessage(prefix + cmp("Successfully enabled Marker Command addition!"))
    }

    private val onDisable = Consumer<BlueMapAPI> {
        consoleAudience.sendMessage(prefix + cmp("Disconnecting from BlueMap API..."))
        MarkerManager.saveAllMarker(sourceFolder)
        configFile.writeText(jsonPretty.encodeToString(settings))
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