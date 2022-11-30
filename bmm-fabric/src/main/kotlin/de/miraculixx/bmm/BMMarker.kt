package de.miraculixx.bmm

import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.message.cmp
import de.miraculixx.bmm.utils.message.consoleAudience
import de.miraculixx.bmm.utils.message.plus
import de.miraculixx.bmm.utils.message.prefix
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarting
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped
import net.kyori.adventure.platform.fabric.FabricServerAudiences
import net.minecraft.server.MinecraftServer
import java.io.File


class BMMarker : ModInitializer {
    private lateinit var blueMapInstance: BlueMap
    private lateinit var config: File

    override fun onInitialize() {
        MarkerCommand()

        ServerLifecycleEvents.SERVER_STARTING.register(ServerStarting { server: MinecraftServer? ->
            val adventure = FabricServerAudiences.of(server!!)
            consoleAudience = adventure.console()
            File("config/bm-marker").mkdirs()
            config = File("config/bm-marker/markers.json")
            blueMapInstance = BlueMap(config)
        })

        ServerLifecycleEvents.SERVER_STOPPED.register(ServerStopped {
            blueMapInstance.disable()
            MarkerManager.saveAllMarker(config)
            consoleAudience.sendMessage(prefix + cmp("Successfully saved all data! Good Bye :)"))
        })
    }
}