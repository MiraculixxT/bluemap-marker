package de.miraculixx.bmm.commands

import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bmm.map.MarkerBuilder
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.map.MarkerManager.builder
import de.miraculixx.bmm.map.MarkerManager.builderSet
import de.miraculixx.bmm.map.MarkerSetBuilder
import de.miraculixx.bmm.map.data.BMarker
import de.miraculixx.bmm.map.data.BMarkerSet
import de.miraculixx.bmm.map.data.Box
import de.miraculixx.bmm.utils.*
import de.miraculixx.bmm.utils.data.PlayerData
import de.miraculixx.bmm.utils.data.mainCommandPrefix
import de.miraculixx.bmm.utils.data.setupCommandPrefix
import de.miraculixx.bmm.utils.data.setupSetCommandPrefix
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.enums.MarkerType
import de.miraculixx.mcommons.extensions.enumOf
import de.miraculixx.mcommons.text.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import java.util.*
import kotlin.jvm.optionals.getOrNull

interface MarkerCommandInstance: MarkerBuilderInstance {
    private val alreadyStarted: Component
        get() = prefix + locale.msg("command.alreadyStarted") +
                cmp(locale.msgCancel(), cError, underlined = true).addCommand("/$setupCommandPrefix cancel") +
                cmp(" ${locale.msgOr()} ", cError) +
                cmp(locale.msgBuild(), cError, underlined = true).addCommand("/$setupCommandPrefix build") +
                locale.msg("command.alreadyStarted")

    /**
     * Create a new marker setup
     * @param id user/console name
     */
    fun create(sender: Audience, id: String, stringType: String?, mapID: String?, setID: String?, worlds: List<String>?, pData: PlayerData?) {
        // Guide through inputs
        val type = enumOf<MarkerType>(stringType?.uppercase())
        if (type == null) {
            sender.sendMessage(prefix + locale.msg("command.notValidMarker", listOf(stringType ?: "Unknown")))
            return
        }
        if (mapID == null) {
            sendMapSelection(sender, worlds ?: emptyList(), "$mainCommandPrefix create $stringType")
            return
        }
        if (setID == null) {
            sendSetSelection(sender, mapID, "$mainCommandPrefix create $stringType $mapID", pData = pData!!)
            return
        }
        if (builder.contains(id)) {
            sender.sendMessage(alreadyStarted)
            return
        }

        // Check permissions
        if (pData?.permSetOther != true) {
            val set = MarkerManager.blueMapMaps[mapID]?.get(setID)
            if (set?.owner != pData?.uuid) {
                sender.sendMessage(prefix + locale.msg("command.notYourSet"))
                return
            }
            if (set != null && settings.maxUserMarker >= 0 && set.markers.count { it.value.owner == pData?.uuid } >= settings.maxUserMarker) {
                sender.sendMessage(prefix + locale.msg("command.maxMarkers", listOf(settings.maxUserMarker.toString())))
                return
            }
        }

        // Logic
        builder[id] = MarkerBuilder(type, mutableMapOf(MarkerArg.MAP to Box.BoxString(mapID), MarkerArg.MARKER_SET to Box.BoxString(setID)))
        sender.sendMessage(
            prefix +
                    locale.msg("command.createMarker", listOf("")) +
                    cmp("/$setupCommandPrefix", cMark, underlined = true).addSuggest("/$setupCommandPrefix ").addHover(cmp("${locale.msgUse()} /$setupCommandPrefix <arg> <value>")) +
                    locale.msg("command.createMarker2") +
                    cmp("/$setupCommandPrefix build", cMark, underlined = true).addCommand("/$setupCommandPrefix build")
        )
        sendStatusInfo(sender, id)
    }

    /**
     * Create a new marker set setup
     * @param id user/console name
     */
    fun createSet(sender: Audience, id: String, mapID: String?, worlds: List<String>?, pData: PlayerData?) {
        // Guide through inputs
        if (mapID == null) {
            sendMapSelection(sender, worlds ?: emptyList(), "$mainCommandPrefix set-create")
            return
        }
        if (builderSet.contains(id)) {
            sender.sendMessage(alreadyStarted)
            return
        }

        // Check permissions
        if (pData?.permSetOther != true) {
            val map = MarkerManager.blueMapMaps[mapID]
            if (map != null && settings.maxUserSets >= 0 && map.count { it.value.owner == pData?.uuid } >= settings.maxUserSets) {
                sender.sendMessage(prefix + locale.msg("command.maxSets", listOf(settings.maxUserSets.toString())))
                return
            }
        }

        // Logic
        builderSet[id] = MarkerSetBuilder(mutableMapOf(MarkerArg.MAP to Box.BoxString(mapID)))
        sender.sendMessage(
            prefix +
                    locale.msg("command.createMarker", listOf("-Set")) +
                    cmp("/$setupSetCommandPrefix", cMark, underlined = true).addSuggest("/$setupSetCommandPrefix ").addHover(cmp("${locale.msgUse()} /$setupSetCommandPrefix <arg> <value>")) +
                    locale.msg("command.createMarker2") +
                    cmp("/$setupSetCommandPrefix build", cMark, underlined = true).addCommand("/$setupSetCommandPrefix build")
        )
        sendStatusInfo(sender, id, true)
    }

    /**
     * Delete a marker
     * @param mapID The map id, not name
     */
    fun delete(sender: Audience, mapID: String?, setID: String?, markerID: String?, worlds: List<String>?, pData: PlayerData?) {
        // Guide through inputs
        if (markerID == null) {
            manageMarkerSelection(sender, worlds ?: emptyList(), "$mainCommandPrefix delete", mapID, setID, pData)
            return
        }

        // Check permissions
        val set = MarkerManager.blueMapMaps[mapID]?.get(setID)
        if (pData?.permMarkerOther != null && pData.uuid != set?.owner) {
            sender.sendMessage(prefix + locale.msg("command.notYourMarker"))
            return
        }

        // Logic
        if (MarkerManager.blueMapMaps[mapID]?.get(setID)?.removeMarker(markerID) == true) {
            sender.sendMessage(prefix + locale.msg("command.deleteMarker", listOf(markerID)))
        } else sender.sendMessage(prefix + locale.msg("command.notValidMarker", listOf(markerID)))
    }

    /**
     * Delete a marker set
     * @param mapID The map id, not name
     */
    fun deleteSet(sender: Audience, confirm: Boolean, setID: String?, mapID: String?, worlds: List<String>?, pData: PlayerData?) {
        // Guide through inputs
        if (mapID == null) {
            sendMapSelection(sender, worlds ?: emptyList(), "$mainCommandPrefix set-delete")
            return
        }
        if (setID == null) {
            sendSetSelection(sender, mapID, "$mainCommandPrefix set-delete $mapID", pData = pData!!)
            return
        }
        if (!confirm) {
            sender.sendMessage(
                prefix + locale.msg("command.confirmDelete", listOf(setID, mapID)) +
                        cmp("/$mainCommandPrefix set-delete $mapID $setID true", cError, underlined = true)
            )
        }

        // Check permissions
        if (pData?.permSetOther != true && MarkerManager.blueMapMaps[mapID]?.get(setID)?.owner != pData?.uuid) {
            sender.sendMessage(prefix + locale.msg("command.notYourSet"))
            return
        }

        // Logic
        if (MarkerManager.removeSet(mapID, setID)) {
            sender.sendMessage(prefix + locale.msg("command.deleteSet", listOf(setID)))
        } else sender.sendMessage(prefix + cmp("This marker-set does not exist or BlueMap is not loaded!", cError))
    }

    /**
     * Finish the marker setup and add a new marker
     * @param id user/console name
     */
    fun build(sender: Audience, id: String, uuid: UUID?) {
        val build = builder[id]
        if (build == null) {
            noBuilder(sender)
            return
        }

        val args = build.getArgs()
        val markerID = args[MarkerArg.ID]?.getString()


        // Apply template data if template
        build.templateSet?.let {
            if (markerID == null || !validateID(markerID)) {
                sender.sendMessage(prefix + locale.msg("command.mustAlphanumeric"))
                return
            }
            if (build.isEdit) {
                // Remove all placed marker -> update template -> place all markers again
                it.playerMarkers.forEach { (_, entry) ->
                    if (entry.templateName != markerID) return@forEach
                    it.unplaceMarker(entry)
                    val newMarker = BMarker(UUID(0, 0), build.getType(), args)
                    it.templateMarker[markerID] = newMarker
                    it.placeMarker(entry, newMarker)
                }
                sender.sendMessage(cmp("\n") + prefix + locale.msg("command.template.updateMarker", listOf(markerID)))
            } else {
                if (it.templateMarker.containsKey(markerID)) {
                    sender.sendMessage(prefix + locale.msg("command.idAlreadyExist", listOf(markerID)))
                    return
                }
                it.templateMarker[markerID] = BMarker(UUID(0, 0), build.getType(), args)
                sender.sendMessage(cmp("\n") + prefix + locale.msg("command.template.addTemplateMarker", listOf(markerID)))
            }
            builder.remove(id)
            return
        }

        // Check if builder is valid
        val mapID = args[MarkerArg.MAP]?.getString()
        val setID = args[MarkerArg.MARKER_SET]?.getString()
        if (mapID == null || setID == null || markerID == null) {
            sender.sendMessage(prefix + locale.msg("command.mustProvideID"))
            return
        }
        if (!validateID(markerID)) {
            sender.sendMessage(prefix + locale.msg("command.mustAlphanumeric"))
            return
        }
        val set = MarkerManager.blueMapMaps[mapID]?.get(setID)
        if (set == null) {
            sender.sendMessage(prefix + locale.msg("command.setNotFound", listOf("$mapID/$setID", mainCommandPrefix)))
            return
        }
        if (set.markers.containsKey(markerID) && !build.isEdit) {
            sender.sendMessage(prefix + locale.msg("command.idAlreadyExist", listOf(markerID)))
            return
        }

        // Check if all important arguments are set
        val marker = build.apply()
        if (marker.second) {
            sender.sendMessage(prefix + locale.msg("command.missingImportant"))
            return
        }

        // Add/edit marker
        if (!build.isEdit) set.addMarker(uuid ?: UUID(0, 0), build, markerID)
        builder.remove(id)
        sender.sendMessage(prefix + locale.msg("command.createdMarker"))
    }

    /**
     * Finish the marker set setup and add a new marker set
     * @param id user/console name
     */
    fun buildSet(sender: Audience, id: String, uuid: UUID?) {
        if (!builderSet.contains(id)) {
            noBuilder(sender, true)
            return
        }

        // Check if builder is valid
        val build = builderSet[id]
        val mapID = build?.getArgs()?.get(MarkerArg.MAP)?.getString()
        val setID = build?.getArgs()?.get(MarkerArg.ID)?.getString()
        if (setID == null || mapID == null) {
            sender.sendMessage(prefix + locale.msg("command.mustProvideIDSet"))
            return
        }
        if (!validateID(setID)) {
            sender.sendMessage(prefix + locale.msg("command.mustAlphanumeric"))
            return
        }
        val map = MarkerManager.blueMapMaps[mapID]
        val blueMapMap = MarkerManager.blueMapAPI?.getMap(mapID)?.get()
        if (map == null || blueMapMap == null) {
            sender.sendMessage(prefix + locale.msg("command.mapNotFound", listOf(mapID, mainCommandPrefix)))
            return
        }
        if (map.contains(setID) && !build.isEdit) {
            sender.sendMessage(prefix + locale.msg("command.idAlreadyExist", listOf(setID)))
            return
        }

        // Create/edit marker set
        if (build.isEdit) {
            build.apply()
            map[setID]?.attributes?.putAll(build.getArgs())
        } else {
            val bMarkerSet = BMarkerSet(uuid ?: UUID(0, 0), build.getArgs(), mutableMapOf())
            bMarkerSet.load(setID, blueMapMap)
        }

        // Send feedback
        builderSet.remove(id)
        sender.sendMessage(
            prefix + locale.msg("command.createdSet") + cmp("/$mainCommandPrefix create", cMark, underlined = true)
                        .addSuggest("/$mainCommandPrefix create ")
                        .addHover(cmp("${locale.msgUse()} /$mainCommandPrefix create <type>"))
        )
    }

    /**
     * Cancel the current marker(-set) setup
     * @param id user/console name
     */
    fun cancel(sender: Audience, id: String, isSet: Boolean = false) {
        val removed = if (isSet) builderSet.remove(id) else builder.remove(id)
        if (removed == null) noBuilder(sender, isSet)
        else sender.sendMessage(prefix + locale.msg("command.canceledSetup", listOf(if (isSet) "-set" else "")))
    }

    /**
     * Edit a marker
     * @param id user/console name
     */
    fun edit(sender: Audience, id: String, mapID: String?, setID: String?, markerID: String?, worlds: List<String>?, pData: PlayerData?) {
        // Guide through inputs
        if (builder.contains(id)) {
            sender.sendMessage(alreadyStarted)
            return
        }
        if (markerID == null) {
            manageMarkerSelection(sender, worlds ?: emptyList(), "$mainCommandPrefix edit", mapID, setID, pData)
            return
        }
        val marker = MarkerManager.blueMapMaps[mapID]?.get(setID)?.markers?.get(markerID)
        if (marker == null) {
            sender.sendMessage(prefix + locale.msg("command.mustProvideID"))
            return
        }

        // Check permissions
        if (pData?.permMarkerOther != true && pData?.uuid != marker.owner) {
            sender.sendMessage(prefix + locale.msg("command.notYourMarker"))
            return
        }

        // Logic
        val build = marker.getEditor()
        if (build == null) {
            sender.sendMessage(prefix + cmp("Failed to edit marker (not loaded). Did BlueMap boot up correctly?", cError))
            return
        }
        builder[id] = build
        sendStatusInfo(sender, id)
    }

    fun editSet(sender: Audience, id: String, mapID: String?, setID: String?, worlds: List<String>?, pData: PlayerData?) {
        // Guide through inputs
        if (builderSet.contains(id)) {
            sender.sendMessage(alreadyStarted)
            return
        }
        if (mapID == null) {
            sendMapSelection(sender, worlds ?: emptyList(), "$mainCommandPrefix set-edit")
            return
        }
        if (setID == null) {
            sendSetSelection(sender, mapID, "$mainCommandPrefix set-edit $mapID", pData = pData!!)
            return
        }
        val set = MarkerManager.blueMapMaps[mapID]?.get(setID)
        if (set == null) {
            sender.sendMessage(prefix + locale.msg("command.mustProvideIDSet"))
            return
        }

        // Check permissions
        if (pData?.permSetOther != true && pData?.uuid != set.owner) {
            sender.sendMessage(prefix + locale.msg("command.notYourSet"))
            return
        }

        // Logic
        val build = set.getEditor()
        if (build == null) {
            sender.sendMessage(prefix + cmp("Failed to edit marker-set (not loaded). Did BlueMap boot up correctly?", cError))
            return
        }
        builderSet[id] = build
        sendStatusInfo(sender, id, true)
    }

    /**
     * Toggle the visibility of a player marker
     */
    fun setPlayerVisibility(sender: Audience, targets: List<Pair<UUID, String>>, visible: Boolean) {
        BlueMapAPI.getInstance().ifPresentOrElse({ api ->
            if (targets.isEmpty()) {
                sender.sendMessage(prefix + locale.msg("command.notValidPlayer"))
                return@ifPresentOrElse
            }
            targets.forEach { target ->
                api.webApp.setPlayerVisibility(target.first, visible)
                val info = if (visible) "<green>visible</green>" else "<red>invisible</red>"
                sender.sendMessage(prefix + locale.msg("command.changedVisibility", listOf(target.second, info)))
            }
        }) {
            sender.sendMessage(prefix + cmp("Failed to connect to BlueMap! Are you using the latest version?", cError))
        }
    }


    /*
     *
     * Begin of Utility functions to display or calculate output
     *
     */
    fun sendAppliedSuccess(sender: Audience, id: String, message: String, isSet: Boolean = false) {
        sendStatusInfo(sender, id, isSet, prefix + cmp("Marker${if (isSet) "-Set" else ""} $message applied!", cSuccess))
        sender.playSound(Sound.sound(Key.key("block.note_block.bit"), Sound.Source.MASTER, 1f, 1.3f))
    }

    fun validateID(id: String): Boolean {
        return id.matches(Regex("[A-Za-z0-9_-]*")) && !id.contains(' ')
    }

    fun setMarkerArgument(sender: Audience, id: String, type: MarkerArg, box: Box, message: String, isSet: Boolean = false) {
        val builder = getBuilder(sender, id, isSet) ?: return
        builder.setArg(type, box)
        sendAppliedSuccess(sender, id, message, isSet)
    }

    fun getMarkerArgument(sender: Audience, id: String, type: MarkerArg, isSet: Boolean = false): Box? {
        val builder = getBuilder(sender, id, isSet) ?: return null
        return builder.getArgs()[type]
    }

    fun changeLanguage(sender: Audience, language: Locale) {
        if (de.miraculixx.bmm.localization?.setLanguage(language) == true) {
            settings.language = language
            sender.sendMessage(prefix + locale.msg("command.switchLang"))
        } else sender.sendMessage(prefix + locale.msg("command.switchLangFailed"))
    }

    fun getLanguageKeys() = de.miraculixx.bmm.localization?.getLoadedKeys() ?: emptyList()

    private fun manageMarkerSelection(sender: Audience, worlds: List<String>?, command: String, mapID: String?, setID: String?, pData: PlayerData?) {
        if (mapID == null) {
            sendMapSelection(sender, worlds ?: emptyList(), command)
            return
        }
        if (setID == null) {
            sendSetSelection(sender, mapID, "$command $mapID", true, pData!!)
            return
        }
        sender.sendMessage(prefix + locale.msg("command.mustProvideID"))
    }

    private fun sendMapSelection(sender: Audience, worlds: List<String>, command: String) {
        sender.sendMessage(cmp("\n\n") + locale.msg("command.selectMap"))
        val api = MarkerManager.blueMapAPI
        if (api == null) {
            sender.sendMessage(prefix + cmp("Failed to connect to BlueMap!", cError))
            return
        }
        worlds.forEach { world ->
            val bWorld = api.getWorld(world)?.getOrNull() ?: return@forEach
            sender.sendMessage(cmp("→ ") + cmp(world[0].uppercase() + world.substring(1).replace('_', ' '), cHighlight, true))
            bWorld.maps.forEach { map ->
                sender.sendMessage(
                    (cmp("  ▪ ") + cmp(map.name, cMark) + cmp(" (click)"))
                        .addHover(cmp("Click to select this map in $world"))
                        .addCommand("/$command ${map.id}")
                )
            }
            sender.sendMessage(emptyComponent())
        }
    }

    private fun sendSetSelection(sender: Audience, mapID: String, command: String, suggest: Boolean = false, pData: PlayerData) {
        val sets = MarkerManager.blueMapMaps[mapID]?.filter { pData.permSetOther || it.value.owner == pData.uuid }
        if (sets.isNullOrEmpty()) {
            sender.sendMessage(prefix + locale.msg("command.noSets", listOf(mapID, mainCommandPrefix)))
            return
        }
        sender.sendMessage(cmp("\n\n") + prefix + locale.msg("command.selectSet"))
        sets.forEach { (setID, data) ->
            var message = (cmp("▪ ") + cmp((data.attributes[MarkerArg.LABEL]?.getString() ?: "Unknown") + " (${data.markers.size})", cMark) + cmp(" (click)"))
                .addHover(cmp("Click to select this marker-set"))
            message = if (suggest) message.addSuggest("/$command $setID ") else message.addCommand("/$command $setID")
            sender.sendMessage(message)
        }
    }
}