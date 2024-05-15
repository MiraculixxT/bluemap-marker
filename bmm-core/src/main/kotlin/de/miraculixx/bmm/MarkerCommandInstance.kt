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
    fun create(sender: Audience, id: String, type: String?) {
        if (builder.contains(id)) {
            sender.sendMessage(alreadyStarted)
        } else {
            val markerType = enumOf<MarkerType>(type?.uppercase())
            if (markerType == null) {
                sender.sendMessage(prefix + locale.msg("command.notValidMarker", listOf(type ?: "Unknown")))
                return
            }
            builder[id] = MarkerBuilder(markerType)
            sender.sendMessage(
                prefix +
                        locale.msg("command.createMarker", listOf("")) +
                        cmp("/$setupCommandPrefix", cMark, underlined = true).addSuggest("/$setupCommandPrefix ").addHover(cmp("${locale.msgUse()} /$setupCommandPrefix <arg> <value>")) +
                        locale.msg("command.createMarker2") +
                        cmp("/$setupCommandPrefix build", cMark, underlined = true).addCommand("/$setupCommandPrefix build")
            )
            sendStatusInfo(sender, id)
        }
    }

    /**
     * Create a new marker set setup
     * @param id user/console id
     */
    fun createSet(sender: Audience, id: String) {
        if (builderSet.contains(id)) {
            sender.sendMessage(
                prefix +
                        locale.msg("command.alreadyStarted") +
                        cmp(locale.msgCancel(), cError, underlined = true).addCommand("/$setupSetCommandPrefix cancel") +
                        cmp(" ${locale.msgOr()} ", cError) +
                        cmp(locale.msgBuild(), cError, underlined = true).addCommand("/$setupSetCommandPrefix build") +
                        locale.msg("command.alreadyStarted")
            )
        } else {
            builderSet[id] = MarkerSetBuilder()
            sender.sendMessage(
                prefix +
                        locale.msg("command.createMarker", listOf("-Set")) +
                        cmp("/$setupSetCommandPrefix", cMark, underlined = true).addSuggest("/$setupSetCommandPrefix ").addHover(cmp("${locale.msgUse()} /$setupSetCommandPrefix <arg> <value>")) +
                        locale.msg("command.createMarker2") +
                        cmp("/$setupSetCommandPrefix build", cMark, underlined = true).addCommand("/$setupSetCommandPrefix build")
            )
            sendStatusInfo(sender, id, true)
        }
    }

    /**
     * Delete a marker
     * @param mapID The map id, not name
     */
    fun delete(sender: Audience, mapID: String, setID: String?, markerID: String?) {
        if (setID == null || markerID == null) {
            invalidData(sender, setID, markerID)
            return
        }

        if (MarkerManager.blueMapMaps[mapID]?.get(setID)?.removeMarker(markerID) == true) {
            sender.sendMessage(prefix + locale.msg("command.deleteMarker", listOf(markerID)))
        } else sender.sendMessage(prefix + locale.msg("command.notValidMarker", listOf(markerID)))
    }

    fun deleteSet(sender: Audience, confirm: Boolean, setID: String?, mapID: String?) {
        if (!confirm) {
            sender.sendMessage(
                prefix + locale.msg("command.confirmDelete", listOf(setID ?: "Unknown", mapID ?: "Unknown")) +
                        cmp("/$mainCommandPrefix set-delete $mapID $setID true", cError, underlined = true)
            )
        }
        if (setID == null || mapID == null) {
            sender.sendMessage(prefix + locale.msg("command.notValidSet", listOf(setID ?: "Unknown", mapID ?: "Unknown")))
            return
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
            val markerSetFull = args?.get(MarkerArg.MARKER_SET)?.getString()?.split('.')
            val mapID = markerSetFull?.getOrNull(0)
            val setID = markerSetFull?.getOrNull(1)
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
            if (set.markers.containsKey(markerID)) {
                sender.sendMessage(prefix + locale.msg("command.idAlreadyExist", listOf(id)))
                return
            }

            val marker = build.apply()
            if (marker.second) {
                sender.sendMessage(prefix + locale.msg("command.missingImportant"))
                return
            }

            set.addMarker(id.toUUID() ?: UUID(0,0), build, markerID)
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

            val bMarkerSet = BMarkerSet(id.toUUID() ?: UUID(0,0), build.getArgs(), mutableMapOf())
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

    fun edit(sender: Audience, id: String, mapID: String?, setID: String?, markerID: String?) {
        if (builder.contains(id)) {
            sender.sendMessage(alreadyStarted)
            return
        }
        if (setID == null || markerID == null || mapID == null) {
            invalidData(sender, setID, markerID)
            return
        }
        val marker = MarkerManager.blueMapMaps[mapID]?.get(setID)?.markers?.get(markerID)
        if (marker == null) {
            sender.sendMessage(
                prefix +
                        cmp("Could not find any marker in set ", cError) +
                        cmp(setID, cError, underlined = true) +
                        cmp(" with ID ", cError) +
                        cmp(markerID, cError, underlined = true) +
                        cmp("!", cError)
            )
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
        sender.sendMessage(cmp(" \n") + cmp("                       ", cHighlight, strikethrough = true) + cmp("[", cHighlight) +
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

            val value = appliedArgs[arg]
            val color = if (value == null) { if (arg.isRequired) cError else NamedTextColor.GRAY } else cSuccess
            sender.sendMessage(dash +
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
        return id.matches(Regex("[A-Za-z0-9]*")) && !id.contains(' ')
    }

    fun setMarkerArgument(sender: Audience, id: String, type: MarkerArg, value: Any?, message: String, isSet: Boolean = false) {
        if (value == null) {
            sender.playSound(Sound.sound(Key.key("entity.enderman.teleport"), Sound.Source.MASTER, 1f, 1f))
            sender.sendMessage(prefix + cmp("Please enter any value!", cError))
            return
        }
        val builder = getBuilder(sender, id, isSet) ?: return
        builder.setArg(type, Box(value))
        sendAppliedSuccess(sender, id, message, isSet)
    }

    fun changeLanguage(sender: Audience, language: Locale) {
        if (localization?.setLanguage(language) == true) {
            settings.language = language
            sender.sendMessage(prefix + locale.msg("command.switchLang"))
        } else sender.sendMessage(prefix + locale.msg("command.switchLangFailed"))
    }

    fun getLanguageKeys() = localization?.getLoadedKeys() ?: emptyList()

    private fun invalidData(sender: Audience, setID: String?, markerID: String?) {
        sender.sendMessage(prefix + cmp("Invalid set-ID or marker-ID! ($setID - $markerID)", cError))
    }
}