package de.miraculixx.bmm.commands

import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.data.teleportCommand
import de.miraculixx.bmm.utils.data.teleportCommandOthers
import de.miraculixx.bmm.utils.data.teleportCommandPrefix
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.mcommons.text.cmp
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import io.papermc.paper.adventure.AdventureComponent
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

class TeleportCommand : TeleportCommandInstance {
    private val command = commandTree(teleportCommandPrefix) {
        withPermission(teleportCommand)

        stringArgument("marker") {
            suggestAllMarkerIDs()
            playerExecutor { player, args ->
                val pos = player.resolveMarker(args[0] as String, player.getMapIDs(), player.uniqueId, player.hasPermission(teleportCommandOthers)) ?: return@playerExecutor
                player.teleportAsync(Location(player.world, pos.x, pos.y, pos.z))
            }
        }
    }

    private fun Player.getMapIDs() =
        BlueMapAPI.getInstance()?.getOrNull()?.getWorld(world)?.getOrNull()?.maps?.map { it.id }?.toSet() ?: emptySet()

    private fun <T> Argument<T>.suggestAllMarkerIDs() = replaceSuggestions { info, builder ->
        CompletableFuture.supplyAsync {
            val player = info.sender as? Player ?: return@supplyAsync null
            val bmaps = player.getMapIDs()
            val maps = MarkerManager.blueMapMaps.filterKeys { it in bmaps }.values

            maps.forEach { map ->
                map.forEach { (setID, set) ->
                    set.markers.forEach { (markerID, marker) ->
                        val isBypass = player.hasPermission(teleportCommandOthers)
                        val isTeleporter = marker.attributes[MarkerArg.TELEPORTER]?.getBoolean() == true
                        if (isTeleporter || isBypass || marker.owner == player.uniqueId) {
                            builder.suggest(markerID, AdventureComponent(cmp("Set: $setID, Label: ${marker.attributes[MarkerArg.LABEL]?.getString() ?: "Unknown"}")))
                        }
                    }
                }
            }
            builder.build()
        }
    }
}