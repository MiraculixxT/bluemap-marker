package de.miraculixx.bmm

import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bmm.api.APIConnector
import de.miraculixx.bmm.api.Loader
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.message.cmp
import de.miraculixx.bmm.utils.message.consoleAudience
import de.miraculixx.bmm.utils.message.plus
import de.miraculixx.bmm.utils.message.prefix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.function.Consumer

class BlueMap(sourceFolder: File, loader: Loader, version: String) {
    private val onEnable = Consumer<BlueMapAPI> {
        consoleAudience.sendMessage(prefix + cmp("Connect to BlueMap API..."))
        MarkerManager.loadAllMarker(it, sourceFolder)
        consoleAudience.sendMessage(prefix + cmp("Successfully enabled Marker Command addition!"))
        CoroutineScope(Dispatchers.Default).launch {
            APIConnector.checkVersion(loader, version)
        }
    }

    private val onDisable = Consumer<BlueMapAPI> {
        consoleAudience.sendMessage(prefix + cmp("Disconnecting from BlueMap API..."))
        MarkerManager.saveAllMarker(sourceFolder)
        consoleAudience.sendMessage(prefix + cmp("Successfully saved all data. Waiting for BlueMap to reload..."))
    }

    fun disable() {
        BlueMapAPI.unregisterListener(onDisable)
        BlueMapAPI.unregisterListener(onEnable)
    }

    init {
        BlueMapAPI.onEnable(onEnable)
        BlueMapAPI.onDisable(onDisable)
    }
}