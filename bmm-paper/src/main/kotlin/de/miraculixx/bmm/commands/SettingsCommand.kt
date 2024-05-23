package de.miraculixx.bmm.commands

import de.miraculixx.bmm.utils.data.manageSettings
import de.miraculixx.bmm.utils.data.settingsCommandPrefix
import de.miraculixx.bmm.utils.settings
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.literalArgument

class SettingsCommand: SettingsCommandInterface {
    private val settingsCommand = commandTree(settingsCommandPrefix) {
        withPermission(manageSettings)

        // TODO
        literalArgument("language") {}

        intSetting("maxUserSets", { settings.maxUserSets }) { settings.maxUserSets = it }
        intSetting("maxUserMarker", { settings.maxUserMarker }) { settings.maxUserMarker = it }
    }

    private fun CommandTree.intSetting(name: String, get: () -> Int, set: (Int) -> Unit) = literalArgument(name) {
        anyExecutor { sender, _ -> sendCurrentInfo(sender, get().toString()) }
        integerArgument("new-value", -1) {
            anyExecutor { sender, args ->
                set(args[0] as Int)
                sendChangedInfo(sender, get().toString())
            }
        }
    }
}