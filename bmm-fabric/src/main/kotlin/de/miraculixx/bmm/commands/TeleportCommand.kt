package de.miraculixx.bmm.commands

import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.data.teleportCommand
import de.miraculixx.bmm.utils.data.teleportCommandOthers
import de.miraculixx.bmm.utils.data.teleportCommandPrefix
import de.miraculixx.bmm.utils.enums.MarkerArg
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import net.silkmc.silk.commands.ArgumentCommandBuilder
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.entity.world
import kotlin.jvm.optionals.getOrNull

class TeleportCommand : TeleportCommandInstance {
    private val command = command(teleportCommandPrefix) {
        requires {
            Permissions.require(teleportCommand, 3).test(it) && it.isPlayer
        }

        argument<String>("marker") { marker ->
            suggestAllMarkerIDs()
            runs {
                val player = source.player ?: return@runs
                val pos = source.resolveMarker(marker(), player.getMapIDs(), player.uuid, Permissions.require(teleportCommandOthers, 3).test(source)) ?: return@runs
                player.teleportTo(pos.x, pos.y, pos.z)
            }
        }
    }

    private fun ServerPlayer.getMapIDs() =
        BlueMapAPI.getInstance()?.getOrNull()?.getWorld(world)?.getOrNull()?.maps?.map { it.id }?.toSet() ?: emptySet()

    private fun <T> ArgumentCommandBuilder<CommandSourceStack, T>.suggestAllMarkerIDs() {
        suggestListWithTooltipsSuspending { info ->
            val player = info.source.player ?: return@suggestListWithTooltipsSuspending emptyList()
            val bmaps = player.getMapIDs()
            val maps = MarkerManager.blueMapMaps.filterKeys { it in bmaps }.values

            return@suggestListWithTooltipsSuspending buildSet {
                maps.forEach { map ->
                    map.forEach { (setID, set) ->
                        set.markers.forEach { (markerID, marker) ->
                            val isBypass = Permissions.require(teleportCommandOthers, 3).test(info.source)
                            val isTeleporter = marker.attributes[MarkerArg.TELEPORTER]?.getBoolean() == true
                            if (isTeleporter || isBypass || marker.owner == player.uuid) {
                                markerID to ("Set: $setID, Label: ${marker.attributes[MarkerArg.LABEL]?.getString() ?: "Unknown"}")
                            }
                        }
                    }
                }
            }
        }
    }
}