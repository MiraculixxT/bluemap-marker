package de.miraculixx.bm_marker

import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bm_marker.commands.MarkerCommand
import de.miraculixx.bm_marker.map.MarkerManager
import net.axay.kspigot.main.KSpigot
import java.util.function.Consumer

class BMMarker : KSpigot() {
    companion object {
        lateinit var INSTANCE: KSpigot
    }

    override fun startup() {
        INSTANCE = this

        // Load Content
        MarkerCommand()

        BlueMapAPI.onEnable(onBlueMapEnable)
        BlueMapAPI.onDisable(onBlueMapDisable)
    }

    override fun shutdown() {
        BlueMapAPI.unregisterListener(onBlueMapEnable)
        BlueMapAPI.unregisterListener(onBlueMapDisable)
        MarkerManager.saveAllMarker()
        logger.info("Successfully saved all data! Good Bye :)")
    }

    private val onBlueMapEnable = Consumer<BlueMapAPI> {
        logger.info("Connect to BlueMap API...")
        MarkerManager.loadAllMarker(it)
        logger.info("Successfully enabled Marker Command addition!")
    }

    private val onBlueMapDisable = Consumer<BlueMapAPI> {
        logger.info("Disconnecting from BlueMap API...")
        MarkerManager.saveAllMarker()
        logger.info("Successfully saved all data. Waiting for BlueMap to reload...")
    }
}

val PluginManager by lazy { BMMarker.INSTANCE }