package de.miraculixx.bmm

import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.Settings
import de.miraculixx.bmm.utils.settings
import de.miraculixx.bmm.utils.sourceFolder
import de.miraculixx.mcommons.extensions.loadConfig
import de.miraculixx.mcommons.extensions.saveConfig
import de.miraculixx.mcommons.text.*
import java.io.File
import java.util.Locale
import java.util.function.Consumer

var localization: Localization? = null

class BlueMap(version: Int) {
    private val configFile = File(sourceFolder, "settings.json")

    private val onEnable = Consumer<BlueMapAPI> {
        consoleAudience.sendMessage(prefix + cmp("Connect to BlueMap API..."))
        settings = configFile.loadConfig(Settings())
        val languages = listOf(Locale.ENGLISH, Locale.GERMAN).map { key -> key to javaClass.getResourceAsStream("/language/$key.yml") }
        localization = Localization(File(sourceFolder, "language"), settings.language, languages)
        MarkerManager.load(it)
        consoleAudience.sendMessage(prefix + cmp("Successfully enabled Marker Command addition!"))
    }

    private val onDisable = Consumer<BlueMapAPI> {
        consoleAudience.sendMessage(prefix + cmp("Disconnecting from BlueMap API..."))
        MarkerManager.save(it)
        configFile.saveConfig(settings)
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