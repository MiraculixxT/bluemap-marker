package de.miraculixx.bmm

import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.message.cmp
import de.miraculixx.bmm.utils.message.consoleAudience
import de.miraculixx.bmm.utils.message.plus
import de.miraculixx.bmm.utils.message.prefix
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BMMarker : JavaPlugin() {
    companion object {
        lateinit var INSTANCE: JavaPlugin
    }
    private lateinit var blueMapInstance: BlueMap
    private lateinit var commandInstance: MarkerCommandInstance

    override fun onEnable() {
        INSTANCE = this
        consoleAudience = Bukkit.getConsoleSender()

        // Load Content
        commandInstance = MarkerCommand()

        // BlueMap Management
        blueMapInstance = BlueMap(dataFolder)
    }

    override fun onDisable() {
        blueMapInstance.disable()
        MarkerManager.saveAllMarker(dataFolder)
        consoleAudience.sendMessage(prefix + cmp("Successfully saved all data! Good Bye :)"))
    }
}

val PluginManager by lazy { BMMarker.INSTANCE }