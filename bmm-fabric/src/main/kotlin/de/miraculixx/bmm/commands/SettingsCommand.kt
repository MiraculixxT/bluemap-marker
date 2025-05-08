package de.miraculixx.bmm.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.data.manageSettings
import de.miraculixx.bmm.utils.data.settingsCommandPrefix
import de.miraculixx.bmm.utils.settings
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.server.executeCommand
import java.io.File

class SettingsCommand : SettingsCommandInterface {
    private val settingsCommand = command(settingsCommandPrefix) {
        requires {
            Permissions.require(manageSettings, 3).test(it)
        }

        literal("convert") {
            literal("oldBMarkers") {
                runsAsync { convertOldMarkers(source, File("config")) }
            }
            literal("bluemapMarkers") {
                argument<String>("map", StringArgumentType.word()) { map ->
                    suggestList { MarkerManager.blueMapMaps.keys }
                    runsAsync {
                        convertIntegratedMarkers(source, File("config"), map())
                        source.server.executeCommand("bluemap reload")
                    }
                }
            }
        }

        // TODO
        literal("language") {}

        intSetting("maxUserSets", { settings.maxUserSets }) { settings.maxUserSets = it }
        intSetting("maxUserMarker", { settings.maxUserMarker }) { settings.maxUserMarker = it }

        literal("config") {
            literal("save") {
                runsAsync {
                    configSave(source)
                }
            }
            literal("load") {
                runsAsync {
                    configLoad(source, true)
                }
            }
        }
    }

    private fun LiteralCommandBuilder<CommandSourceStack>.intSetting(name: String, get: () -> Int, set: (Int) -> Unit) = literal(name) {
        runs { sendCurrentInfo(source, get().toString()) }
        argument<Int>("new-value", IntegerArgumentType.integer(-1)) { value ->
            runs {
                set(value())
                sendChangedInfo(source, get().toString())
            }
        }
    }
}