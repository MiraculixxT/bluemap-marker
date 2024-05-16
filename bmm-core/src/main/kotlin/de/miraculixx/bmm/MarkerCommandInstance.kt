package de.miraculixx.bmm

import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bmm.map.MarkerBuilder
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.map.MarkerSetBuilder
import de.miraculixx.bmm.map.data.BMarkerSet
import de.miraculixx.bmm.map.data.Box
import de.miraculixx.bmm.map.interfaces.Builder
import de.miraculixx.bmm.utils.*
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.enums.MarkerType
import de.miraculixx.mcommons.extensions.toUUID
import de.miraculixx.mcommons.text.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.util.*
import kotlin.jvm.optionals.getOrNull

interface MarkerCommandInstance {
    val builder: MutableMap<String, MarkerBuilder>
    val builderSet: MutableMap<String, MarkerSetBuilder>
    val mainCommandPrefix: String
        get() = "bmarker"
    val setupCommandPrefix: String
        get() = "bmarker-setup"
    val setupSetCommandPrefix: String
        get() = "bmarker-setup-set"
    val visibilityCommandPrefix: String
        get() = "bplayer"

    private val alreadyStarted: Component
        get() = prefix + locale.msg("command.alreadyStarted") +
                cmp(locale.msgCancel(), cError, underlined = true).addCommand("/$setupCommandPrefix cancel") +
                cmp(" ${locale.msgOr()} ", cError) +
                cmp(locale.msgBuild(), cError, underlined = true).addCommand("/$setupCommandPrefix build") +
                locale.msg("command.alreadyStarted")

    /**
     * Create a new marker setup
     * @param id user/console id
     */
    fun create(sender: Audience, id: String, stringType: String?, mapID: String?, setID: String?, worlds: List<String>?) {
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
            sendSetSelection(sender, mapID, "$mainCommandPrefix create $stringType $mapID")
            return
        }
        if (builder.contains(id)) {
            sender.sendMessage(alreadyStarted)
            return
        }

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
     * @param id user/console id
     */
    fun createSet(sender: Audience, id: String, mapID: String?, worlds: List<String>?) {
        if (mapID == null) {
            sendMapSelection(sender, worlds ?: emptyList(), "$mainCommandPrefix set-create")
            return
        }
        if (builderSet.contains(id)) {
            sender.sendMessage(alreadyStarted)
            return
        }

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
    fun delete(sender: Audience, mapID: String?, setID: String?, markerID: String?, worlds: List<String>?) {
        if (markerID == null) {
            manageMarkerSelection(sender, worlds ?: emptyList(), "$mainCommandPrefix delete", mapID, setID)
            return
        }

        if (MarkerManager.blueMapMaps[mapID]?.get(setID)?.removeMarker(markerID) == true) {
            sender.sendMessage(prefix + locale.msg("command.deleteMarker", listOf(markerID)))
        } else sender.sendMessage(prefix + locale.msg("command.notValidMarker", listOf(markerID)))
    }

    fun deleteSet(sender: Audience, confirm: Boolean, setID: String?, mapID: String?, worlds: List<String>?) {
        if (mapID == null) {
            sendMapSelection(sender, worlds ?: emptyList(), "$mainCommandPrefix set-delete")
            return
        }
        if (setID == null) {
            sendSetSelection(sender, mapID, "$mainCommandPrefix set-delete $mapID")
            return
        }
        if (!confirm) {
            sender.sendMessage(
                prefix + locale.msg("command.confirmDelete", listOf(setID, mapID)) +
                        cmp("/$mainCommandPrefix set-delete $mapID $setID true", cError, underlined = true)
            )
        }
        if (MarkerManager.removeSet(mapID, setID)) {
            sender.sendMessage(prefix + locale.msg("command.deleteSet", listOf(setID)))
        } else sender.sendMessage(prefix + cmp("This marker-set does not exist or BlueMap is not loaded!", cError))
    }

    fun build(sender: Audience, id: String) {
        if (!builder.contains(id)) noBuilder(sender)
        else {
            val build = builder[id]
            val args = build?.getArgs()
            val mapID = args?.get(MarkerArg.MAP)?.getString()
            val setID = args?.get(MarkerArg.MARKER_SET)?.getString()
            val markerID = build?.getArgs()?.get(MarkerArg.ID)?.getString()
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
                sender.sendMessage(prefix + locale.msg("command.idAlreadyExist", listOf(id)))
                return
            }

            val marker = build.apply()
            if (marker.second) {
                sender.sendMessage(prefix + locale.msg("command.missingImportant"))
                return
            }

            if (!build.isEdit) set.addMarker(id.toUUID() ?: UUID(0, 0), build, markerID)
            builder.remove(id)
            sender.sendMessage(prefix + locale.msg("command.createdMarker"))
        }
    }

    fun buildSet(sender: Audience, id: String) {
        if (!builderSet.contains(id)) noBuilder(sender, true)
        else {
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
            if (map.contains(setID)) {
                sender.sendMessage(prefix + locale.msg("command.idAlreadyExist", listOf(id)))
                return
            }

            val bMarkerSet = BMarkerSet(id.toUUID() ?: UUID(0, 0), build.getArgs(), mutableMapOf())
            bMarkerSet.load(setID, blueMapMap)
            builderSet.remove(id)

            sender.sendMessage(
                prefix + locale.msg("command.createdSet") +
                        cmp("/$mainCommandPrefix create", cMark, underlined = true).addSuggest("/$mainCommandPrefix create ").addHover(cmp("${locale.msgUse()} /$mainCommandPrefix create <type>"))
            )
        }
    }

    fun cancel(sender: Audience, id: String, isSet: Boolean = false) {
        val removed = if (isSet) builderSet.remove(id) else builder.remove(id)
        if (removed == null) noBuilder(sender, isSet)
        else sender.sendMessage(prefix + locale.msg("command.canceledSetup", listOf(if (isSet) "-set" else "")))
    }

    fun edit(sender: Audience, id: String, mapID: String?, setID: String?, markerID: String?, worlds: List<String>?) {
        if (builder.contains(id)) {
            sender.sendMessage(alreadyStarted)
            return
        }
        if (markerID == null) {
            manageMarkerSelection(sender, worlds ?: emptyList(), "$mainCommandPrefix edit", mapID, setID)
            return
        }
        val marker = MarkerManager.blueMapMaps[mapID]?.get(setID)?.markers?.get(markerID)
        if (marker == null) {
            sender.sendMessage(prefix + locale.msg("command.mustProvideID"))
            return
        }

        val build = marker.getEditor()
        if (build == null) {
            sender.sendMessage(prefix + cmp("Failed to edit marker (not loaded). Did BlueMap boot up correctly?", cError))
            return
        }
        builder[id] = build
        sendStatusInfo(sender, id)
    }

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
    fun noBuilder(sender: Audience, isSet: Boolean = false) {
        val addition = if (isSet) "set-" else ""
        sender.sendMessage(
            prefix +
                    cmp("You have no current marker$addition setups. Start one with ", cError) +
                    cmp("/$mainCommandPrefix ${addition}create", cError, underlined = true).addSuggest("/$mainCommandPrefix ${addition}create ").addHover(cmp("Start a marker$addition setup (click)"))
        )
    }

    fun getBuilder(sender: Audience, id: String, isSet: Boolean = false): Builder? {
        return if (isSet) {
            builderSet.getOrElse(id) {
                noBuilder(sender, true)
                return null
            }
        } else {
            builder.getOrElse(id) {
                noBuilder(sender, true)
                return null
            }
        }
    }

    fun sendStatusInfo(sender: Audience, id: String, isMarkerSet: Boolean = false) {
        val builder = getBuilder(sender, id, isMarkerSet) ?: return
        val type = builder.getType()
        val appliedArgs = builder.getArgs()
        val nothingSet = cmp(locale.msgNotSet(), italic = true)
        val dash = cmp("- ")
        val midDash = cmp(" ≫ ", NamedTextColor.DARK_GRAY)
        val cmd = if (isMarkerSet) "/$setupSetCommandPrefix" else "/$setupCommandPrefix"
        val hoverAddition = cmp("\n\n" + locale.msgString("event.clickToAdd"), cMark)

        val pages = type.args.chunked(6)
        val currentPage = pages.getOrElse(builder.page) { pages.first() }

        // Send header (no worries, I can't read that either)
        sender.sendMessage(
            cmp(" \n") + cmp("                       ", cHighlight, strikethrough = true) + cmp("[", cHighlight) +
                    (if (builder.page > 0) cmp(" ← ", cSuccess).addCommand("/$cmd page-prev").addHover(cmp("Previous Page")) else cmp(" ← ", cError)) +
                    cmp("${builder.page + 1}", cHighlight, true) + cmp("/") + cmp("${pages.size}", cHighlight, true) +
                    (if (builder.page < pages.size - 1) cmp(" → ", cSuccess).addCommand("/$cmd page-next").addHover(cmp("Next Page")) else cmp(" → ", cError)) +
                    cmp("]", cHighlight) + cmp("                       ", cHighlight, strikethrough = true)
        )

        // Send visible arguments
        currentPage.forEach { arg ->
            // List values displayed in a different way than single values
            if (arg.isList) {
                val list = when (arg) {
                    MarkerArg.ADD_POSITION -> appliedArgs[arg]?.getVector3dList() ?: emptyList()
                    MarkerArg.ADD_EDGE -> appliedArgs[arg]?.getVector2dList() ?: emptyList()
                    else -> emptyList()
                }
                val isSet = list.isNotEmpty()
                val color = if (!isSet) cError else cSuccess
                // - ARG >> [3 Values] [+] [-]
                // - ARG >> Not Set
                val inputText = if (isSet) {
                    cmp("[${list.size} Values]", cMark) +
                            cmp(" [+]", cSuccess).addSuggest("$cmd ${arg.name.lowercase()} ").addHover(hoverAddition) +
                            cmp(" [-]", cError).addCommand("$cmd ${arg.name.lowercase()} remove-last").addHover(cmp("Remove last value"))
                } else nothingSet.addSuggest("$cmd ${arg.name.lowercase()} ").addHover(hoverAddition)
                sender.sendMessage(dash + cmp(locale.msgString("arg.${arg.name}"), color) + midDash + inputText)
                return@forEach
            }

            val value = appliedArgs[arg]?.getString()
            val color = if (value == null) {
                if (arg.isRequired) cError else NamedTextColor.GRAY
            } else cSuccess
            sender.sendMessage(
                dash +
                        (cmp(locale.msgString("arg.${arg.name}"), color) + midDash +
                                if (value != null) cmp("$value", cMark) else nothingSet)
                            .addSuggest("$cmd ${arg.name.lowercase()} ")
                            .addHover(cmp(locale.msgString("arg-desc.${arg.name}")) + hoverAddition)
            )
        }

        // Send footer
        sender.sendMessage(
            cmp("                 ", cHighlight, strikethrough = true) + cmp("[ ", cHighlight) +
                    cmp(locale.msgBuild().uppercase(), cSuccess, true, strikethrough = false).addCommand("$cmd build").addHover(cmp(locale.msgString("event.buildHover"))) +
                    cmp(" | ") +
                    cmp(locale.msgCancel().uppercase(), cError, bold = true, strikethrough = false).addCommand("$cmd cancel").addHover(cmp(locale.msgString("event.cancelHover"))) +
                    cmp(" ]", cHighlight) + cmp("                 ", cHighlight, strikethrough = true)
        )
    }

    fun sendAppliedSuccess(sender: Audience, id: String, message: String, isSet: Boolean = false) {
        sender.sendMessage(prefix + cmp("Marker${if (isSet) "-Set" else ""} $message applied!", cSuccess))
        sendStatusInfo(sender, id, isSet)
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
        val value = builder.getArgs()[type]
        if (value == null) {
            sender.sendMessage(prefix + cmp("No value set for this argument!", cError))
            return null
        }
        return value
    }

    fun changeLanguage(sender: Audience, language: Locale) {
        if (localization?.setLanguage(language) == true) {
            settings.language = language
            sender.sendMessage(prefix + locale.msg("command.switchLang"))
        } else sender.sendMessage(prefix + locale.msg("command.switchLangFailed"))
    }

    fun getLanguageKeys() = localization?.getLoadedKeys() ?: emptyList()

    private fun manageMarkerSelection(sender: Audience, worlds: List<String>?, command: String, mapID: String?, setID: String?) {
        if (mapID == null) {
            sendMapSelection(sender, worlds ?: emptyList(), command)
            return
        }
        if (setID == null) {
            sendSetSelection(sender, mapID, "$command $mapID", true)
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
            sender.sendMessage(cmp("→ ") + cmp(world[0].uppercase() + world.substring(1).replace('_',' '), cHighlight, true))
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

    private fun sendSetSelection(sender: Audience, mapID: String, command: String, suggest: Boolean = false) {
        val sets = MarkerManager.blueMapMaps[mapID]
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