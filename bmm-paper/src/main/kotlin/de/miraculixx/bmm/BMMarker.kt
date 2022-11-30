package de.miraculixx.bmm

import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.message.cmp
import de.miraculixx.bmm.utils.message.consoleAudience
import de.miraculixx.bmm.utils.message.plus
import de.miraculixx.bmm.utils.message.prefix
import de.miraculixx.kpaper.extensions.console
import de.miraculixx.kpaper.main.KSpigot

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
    }

    override fun shutdown() {
        blueMapInstance.disable()
        MarkerManager.saveAllMarker(dataFolder)
        consoleAudience.sendMessage(prefix + cmp("Successfully saved all data! Good Bye :)"))
    }
}