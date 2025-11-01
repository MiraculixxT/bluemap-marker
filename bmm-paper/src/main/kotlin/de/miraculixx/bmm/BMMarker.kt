package de.miraculixx.bmm

import de.miraculixx.bmm.commands.MarkerCommand
import de.miraculixx.bmm.commands.SettingsCommand
import de.miraculixx.bmm.commands.TeleportCommand
import de.miraculixx.bmm.commands.TemplateCommand
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.sourceFolder
import de.miraculixx.mcommons.debug
import de.miraculixx.mcommons.text.*
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.jorel.commandapi.CommandAPIPaperConfig
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
        prefix = cmp("BMarker", cHighlight) + _prefixSeparator
        debug = true
        sourceFolder = dataFolder

        // Load Content
        CommandAPI.onLoad(
            CommandAPIPaperConfig(this)
            .silentLogs(true)
            .fallbackToLatestNMS(true)
        )

        MarkerCommand()
        SettingsCommand()
        TeleportCommand()
        MarkerManager.templateLoader = TemplateCommand()

        // BlueMap Management
        blueMapInstance = BlueMap(description.version.toIntOrNull() ?: 0, false)
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
