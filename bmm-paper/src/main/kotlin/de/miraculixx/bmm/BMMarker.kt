package de.miraculixx.bmm

import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.mcommons.text.*
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class BMMarker : JavaPlugin() {
    companion object {
        lateinit var INSTANCE: JavaPlugin
    }

    private lateinit var blueMapInstance: BlueMap

    override fun onLoad() {
        INSTANCE = this
        consoleAudience = server.consoleSender
        prefix = cmp("BMMarker", cHighlight) + _prefixSeparator

        // Load Content
        CommandAPI.onLoad(CommandAPIBukkitConfig(this).silentLogs(true))
        MarkerCommand()

        // BlueMap Management
        blueMapInstance = BlueMap(dataFolder, description.version.toIntOrNull() ?: 0)
    }

    override fun onEnable() {
        CommandAPI.onEnable()
    }

    override fun onDisable() {
        blueMapInstance.disable()
        if (MarkerManager.blueMapAPI?.let { MarkerManager.save(it) } != null)
            consoleAudience.sendMessage(prefix + cmp("Successfully saved all data! Good Bye :)"))
        else consoleAudience.sendMessage(prefix + cmp("Failed to save data!", cError))
        CommandAPI.onDisable()
    }
}