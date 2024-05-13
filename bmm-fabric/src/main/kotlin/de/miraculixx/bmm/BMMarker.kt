package de.miraculixx.bmm

import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.mcommons.text.cmp
import de.miraculixx.mcommons.text.consoleAudience
import de.miraculixx.mcommons.text.plus
import de.miraculixx.mcommons.text.prefix
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.platform.fabric.FabricServerAudiences
import net.minecraft.server.MinecraftServer
import java.io.File

class BMMarker : ModInitializer {
    private lateinit var blueMapInstance: BlueMap
    private lateinit var config: File

    override fun onInitialize() {
        MarkerCommand()
        ServerLifecycleEvents.SERVER_STARTING.register(ServerLifecycleEvents.ServerStarting { server: MinecraftServer? ->
            val adventure = FabricServerAudiences.of(server!!)
            consoleAudience = adventure.console()
            config = File("config/bm-marker")
            if (!config.exists()) config.mkdirs()
            val container = FabricLoader.getInstance().getModContainer("bm-marker").get()
            blueMapInstance = BlueMap(config, container.metadata.version.friendlyString.toIntOrNull() ?: 0)
            GlobalListener
        })

        ServerLifecycleEvents.SERVER_STOPPED.register(ServerLifecycleEvents.ServerStopped {
            blueMapInstance.disable()
            MarkerManager.saveAllMarker(config)
            consoleAudience.sendMessage(prefix + cmp("Successfully saved all data! Good Bye :)"))
        })
    }
}