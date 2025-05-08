package de.miraculixx.bmm.commands

import de.miraculixx.bmm.anyExecutorAsync
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.data.manageSettings
import de.miraculixx.bmm.utils.data.settingsCommandPrefix
import de.miraculixx.bmm.utils.settings
import de.miraculixx.kpaper.extensions.bukkit.dispatchCommand
import de.miraculixx.kpaper.extensions.console
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.*
import java.io.File

class SettingsCommand : SettingsCommandInterface {
    private val settingsCommand = commandTree(settingsCommandPrefix) {
        withPermission(manageSettings)

        // TODO
        literalArgument("language") {}

        literalArgument("convert") {
            literalArgument("oldBMarkers") {
                anyExecutorAsync { sender, _ -> convertOldMarkers(sender, File("plugins")) }
            }
            literalArgument("bluemapMarkers") {
                stringArgument("map") {
                    replaceSuggestions(ArgumentSuggestions.stringCollection { MarkerManager.blueMapMaps.keys })
                    anyExecutorAsync { sender, args ->
                        convertIntegratedMarkers(sender, File("plugins"), args[0] as String)
                        console.dispatchCommand("bluemap reload")
                    }
                }
            }
        }

        intSetting("maxUserSets", { settings.maxUserSets }) { settings.maxUserSets = it }
        intSetting("maxUserMarker", { settings.maxUserMarker }) { settings.maxUserMarker = it }

        literalArgument("config") {
            literalArgument("save") {
                anyExecutorAsync { sender, _ ->
                    configSave(sender)
                }
            }
            literalArgument("load") {
                anyExecutorAsync { sender, _ ->
                    configLoad(sender, false)
                }
            }
        }
    }

    private fun CommandTree.intSetting(name: String, get: () -> Int, set: (Int) -> Unit) = literalArgument(name) {
        anyExecutor { sender, _ -> sendCurrentInfo(sender, get().toString()) }
        integerArgument("new-value", -1) {
            anyExecutorAsync { sender, args ->
                set(args[0] as Int)
                sendChangedInfo(sender, get().toString())
            }
        }
    }
}