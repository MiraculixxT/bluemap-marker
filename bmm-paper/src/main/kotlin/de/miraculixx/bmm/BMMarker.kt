package de.miraculixx.bmm

import de.miraculixx.bmm.utils.message.cmp
import de.miraculixx.bmm.utils.message.consoleAudience
import de.miraculixx.bmm.utils.message.plus
import de.miraculixx.bmm.utils.message.prefix
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

        // Load Content
        CommandAPI.onLoad(CommandAPIBukkitConfig(this).silentLogs(true))
        MarkerCommand()

        // BlueMap Management
        blueMapInstance = BlueMap(dataFolder, description.version.toIntOrNull() ?: 0)
    }

    override fun onEnable() {
        server.pluginManager.registerEvents(GlobalListener, this)
        CommandAPI.onEnable()
    }

    override fun onDisable() {
        blueMapInstance.disable()
        MarkerManager.saveAllMarker(dataFolder)
        consoleAudience.sendMessage(prefix + cmp("Successfully saved all data! Good Bye :)"))
        CommandAPI.onDisable()
    }
}