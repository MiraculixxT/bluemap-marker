package de.miraculixx.bmm.commands

import com.flowpowered.math.vector.Vector3d
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.locale
import de.miraculixx.mcommons.text.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import java.util.UUID

interface TeleportCommandInstance {

    fun Audience.resolveMarker(markerID: String, mapIDs: Set<String>, owner: UUID, isBypass: Boolean): Vector3d? {
        val maps = MarkerManager.blueMapMaps.filterKeys { it in mapIDs }.values
        if (maps.isEmpty()) {
            sendMessage(prefix + locale.msg("command.mapNotFound", listOf(mapIDs.joinToString(", "))))
            return null
        }

        maps.forEach { map ->
            map.forEach { (_, set) ->
                set.markers.forEach { (id, marker) ->
                    if (markerID != id) return@forEach
                    val isBypass = isBypass
                    val isTeleporter = marker.attributes[MarkerArg.TELEPORTER]?.getBoolean() == true
                    if (isTeleporter || isBypass || marker.owner == owner) {
                        sendMessage(prefix + locale.msg("command.teleportMarker", listOf(markerID)))
                        playSound(Sound.sound(Key.key("entity.enderman.teleport"), Sound.Source.MASTER, 0.6f, 1.1f))
                        return marker.attributes[MarkerArg.POSITION]?.getVector3d()
                    }
                }
            }
        }

        sendMessage(prefix + locale.msg("command.notTeleportMarker", listOf(markerID)))
        return null
    }
}