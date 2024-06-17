package de.miraculixx.bmm

import de.miraculixx.bmm.commands.MarkerCommand
import de.miraculixx.bmm.commands.SettingsCommand
import de.miraculixx.bmm.commands.TemplateCommand
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.sourceFolder
import de.miraculixx.mcommons.debug
import de.miraculixx.mcommons.text.*
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.platform.fabric.FabricServerAudiences
import net.minecraft.server.MinecraftServer
import java.io.File

class BMMarker : ModInitializer {
    private lateinit var blueMapInstance: BlueMap

    override fun onInitialize() {
        prefix = cmp("BMarker", cHighlight) + _prefixSeparator
        debug = true
        sourceFolder = File("config/BMMarker")
        if (!sourceFolder.exists()) sourceFolder.mkdirs()

        MarkerManager
        MarkerCommand()
        SettingsCommand()
        MarkerManager.templateLoader = TemplateCommand()

        ServerLifecycleEvents.SERVER_STARTING.register(ServerLifecycleEvents.ServerStarting { server: MinecraftServer? ->
            val adventure = FabricServerAudiences.of(server!!)
            consoleAudience = adventure.console()

            val container = FabricLoader.getInstance().getModContainer("bmmarker").get()
            blueMapInstance = BlueMap(container.metadata.version.friendlyString.toIntOrNull() ?: 0)
        })

        ServerLifecycleEvents.SERVER_STOPPED.register(ServerLifecycleEvents.ServerStopped {
            blueMapInstance.disable()
            if (MarkerManager.blueMapAPI?.let { MarkerManager.save(it) } != null)
                consoleAudience.sendMessage(prefix + cmp("Successfully saved all data! Good Bye :)"))
            else consoleAudience.sendMessage(prefix + cmp("Failed to save data!", cError))
        })
    }
}