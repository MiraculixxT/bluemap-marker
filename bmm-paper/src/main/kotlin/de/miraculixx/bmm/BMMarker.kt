package de.miraculixx.bmm

import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.message.cmp
import de.miraculixx.bmm.utils.message.consoleAudience
import de.miraculixx.bmm.utils.message.plus
import de.miraculixx.bmm.utils.message.prefix
import net.axay.kspigot.extensions.console
import net.axay.kspigot.main.KSpigot

@Suppress("unused")
class BMMarker : KSpigot() {
    companion object {
        lateinit var INSTANCE: KSpigot
    }
    private lateinit var blueMapInstance: BlueMap

    override fun startup() {
        INSTANCE = this
        consoleAudience = console

        // Load Content
        MarkerCommand()

        // BlueMap Management
        blueMapInstance = BlueMap(dataFolder)
        BlueMapAPI.onEnable(blueMapInstance.onEnable)
        BlueMapAPI.onDisable(blueMapInstance.onDisable)
    }

    override fun shutdown() {
        BlueMapAPI.unregisterListener(blueMapInstance.onDisable)
        BlueMapAPI.unregisterListener(blueMapInstance.onEnable)
        MarkerManager.saveAllMarker(dataFolder)
        consoleAudience.sendMessage(prefix + cmp("Successfully saved all data! Good Bye :)"))
    }
}