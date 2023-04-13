package de.miraculixx.bmm

import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.message.cmp
import de.miraculixx.bmm.utils.message.consoleAudience
import de.miraculixx.bmm.utils.message.plus
import de.miraculixx.bmm.utils.message.prefix
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIConfig
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

        // Load Content
        CommandAPI.onLoad(CommandAPIConfig().silentLogs(true))
        MarkerCommand()

        // BlueMap Management
        blueMapInstance = BlueMap(dataFolder)
    }

    override fun onEnable() {
        CommandAPI.onEnable(this)
    }

    override fun onDisable() {
        blueMapInstance.disable()
        MarkerManager.saveAllMarker(dataFolder)
        consoleAudience.sendMessage(prefix + cmp("Successfully saved all data! Good Bye :)"))
        CommandAPI.onDisable()
    }
}