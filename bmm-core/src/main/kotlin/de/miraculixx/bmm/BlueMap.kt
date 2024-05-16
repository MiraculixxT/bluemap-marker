package de.miraculixx.bmm

import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.Settings
import de.miraculixx.bmm.utils.settings
import de.miraculixx.bmm.utils.sourceFolder
import de.miraculixx.mcommons.extensions.loadConfig
import de.miraculixx.mcommons.serializer.jsonPretty
import de.miraculixx.mcommons.text.*
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.Locale
import java.util.function.Consumer

var localization: Localization? = null

class BlueMap(folder: File, version: Int) {
    private val configFile = File(folder, "settings.json")

    private val onEnable = Consumer<BlueMapAPI> {
        consoleAudience.sendMessage(prefix + cmp("Connect to BlueMap API..."))
        settings.apply {
            val s = configFile.loadConfig(Settings())
            language = s.language
        }
        val languages = listOf(Locale.ENGLISH, Locale.GERMAN).map { key -> key to javaClass.getResourceAsStream("/language/$key.yml") }
        localization = Localization(File(folder, "language"), settings.language, languages)
        MarkerManager.load(it)
        consoleAudience.sendMessage(prefix + cmp("Successfully enabled Marker Command addition!"))
    }

    private val onDisable = Consumer<BlueMapAPI> {
        consoleAudience.sendMessage(prefix + cmp("Disconnecting from BlueMap API..."))
        MarkerManager.save(it)
        configFile.writeText(jsonPretty.encodeToString(settings))
        consoleAudience.sendMessage(prefix + cmp("Successfully saved all data. Waiting for BlueMap to reload..."))
    }

    fun disable() {
        BlueMapAPI.unregisterListener(onDisable)
        BlueMapAPI.unregisterListener(onEnable)
    }

    init {
        sourceFolder = folder
        BlueMapAPI.onEnable(onEnable)
        BlueMapAPI.onDisable(onDisable)
    }
}