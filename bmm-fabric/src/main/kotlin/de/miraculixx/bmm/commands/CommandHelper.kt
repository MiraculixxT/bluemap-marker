package de.miraculixx.bmm.commands

import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.utils.data.manageOwnMarkers
import de.miraculixx.bmm.utils.data.manageOwnSets
import de.miraculixx.bmm.utils.enums.MarkerArg
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import net.silkmc.silk.commands.ArgumentCommandBuilder
import net.silkmc.silk.core.text.literalText
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.mapNotNull
import kotlin.jvm.optionals.getOrNull

object CommandHelper {
    fun <T> ArgumentCommandBuilder<CommandSourceStack, T>.suggestMapIDs() {
        suggestListWithTooltipsSuspending {
            val api = MarkerManager.blueMapAPI
            MarkerManager.blueMapMaps.map { (mapID, _) ->
                mapID to literalText(api?.getMap(mapID)?.getOrNull()?.name ?: "Unknown") { color = 0x6e94ff }
            }
        }
    }

    fun <T> ArgumentCommandBuilder<CommandSourceStack, T>.suggestSetIDs(mapIDArgument: String?, mapIDOverride: String? = null) {
        suggestListWithTooltipsSuspending { info ->
            val api = MarkerManager.blueMapAPI
            val mapID = mapIDOverride ?: info.getArgument(mapIDArgument, String::class.java)
            val set = MarkerManager.blueMapMaps[mapID]
            val uuid = info.source.player?.uuid
            val allowOthers = Permissions.require(manageOwnSets, 3).test(info.source)
            set?.mapNotNull { (setID, data) ->
                if (data.owner != uuid && !allowOthers) return@mapNotNull null
                if (setID.startsWith("template_")) return@mapNotNull null // Exclude template sets from indexing
                val mapName = api?.getMap(mapID)?.getOrNull()?.name ?: "Unknown"
                setID to literalText("Map: $mapName, Set: ${data.attributes[MarkerArg.LABEL]?.getString() ?: "Unknown"}")
            }
        }
    }

    fun <T> ArgumentCommandBuilder<CommandSourceStack, T>.suggestMarkerIDs(mapIDArgument: String, setIDArgument: String) {
        suggestListWithTooltipsSuspending { info ->
            val mapID = info.getArgument(mapIDArgument, String::class.java)
            val setID = info.getArgument(setIDArgument, String::class.java)
            val uuid = info.source.player?.uuid
            val allowOthers = Permissions.require(manageOwnMarkers, 2).test(info.source)
            MarkerManager.blueMapMaps[mapID]?.get(setID)?.markers?.mapNotNull { (id, data) ->
                if (data.owner != uuid && !allowOthers) return@mapNotNull null
                id to literalText(data.attributes[MarkerArg.LABEL]?.getString() ?: "Unknown")
            }
        }
    }
}