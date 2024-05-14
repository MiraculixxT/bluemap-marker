package de.miraculixx.bmm

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector3d
import de.bluecolored.bluemap.api.BlueMapAPI
import de.miraculixx.bmm.map.MarkerBuilder
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.map.MarkerSetBuilder
import de.miraculixx.bmm.map.data.ArgumentValue
import de.miraculixx.bmm.map.interfaces.Builder
import de.miraculixx.bmm.utils.*
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.enums.MarkerType
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

    /*
     *
     * Begin of command functions
     *
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

    fun delete(sender: Audience, mapName: String, setID: String?, markerID: String?) {
        if (setID == null || markerID == null) {
            invalidData(sender, setID, markerID)
            return
        }

        if (MarkerManager.removeMarker("${setID}_$mapName", markerID)) {
            sender.sendMessage(prefix + locale.msg("command.deleteMarker", listOf(markerID)))
        } else sender.sendMessage(prefix + locale.msg("command.notValidMarker", listOf(markerID)))
    }

    fun confirmDelete(sender: Audience, setID: String, mapName: String) {
        sender.sendMessage(
            prefix +
                    locale.msg("command.confirmDelete", listOf(setID, mapName)) +
                    cmp("/$mainCommandPrefix set-delete $mapName $setID true", cError, underlined = true)
        )
    }

    fun deleteSet(sender: Audience, confirm: Boolean, setID: String?, mapName: String?) {
        if (!confirm) return
        if (setID == null || mapName == null) {
            sender.sendMessage(prefix + locale.msg("command.notValidSet", listOf(setID ?: "Unknown", mapName ?: "Unknown")))
            return
        }
        if (MarkerManager.removeSet(setID, mapName)) {
            sender.sendMessage(prefix + locale.msg("command.deleteSet", listOf(setID)))
        } else sender.sendMessage(prefix + cmp("This marker-set does not exist or BlueMap is not loaded!", cError))
    }

    fun build(sender: Audience, id: String) {
        if (!builder.contains(id)) noBuilder(sender)
        else {
            val build = builder[id]
            val markerSet = build?.getArgs()?.get(MarkerArg.MARKER_SET)?.getString()
            val markerID = build?.getArgs()?.get(MarkerArg.ID)?.getString()
            if (markerSet == null || markerID == null) {
                sender.sendMessage(prefix + locale.msg("command.mustProvideID"))
                return
            }
            if (!validateID(markerID)) {
                sender.sendMessage(prefix + locale.msg("command.mustAlphanumeric"))
                return
            }
            if (MarkerManager.getAllMarkers(markerSet).contains(markerID)) {
                sender.sendMessage(prefix + locale.msg("command.idAlreadyExist", listOf(id)))
                sender.sendMessage(prefix + locale.msg("command.markerReplaced"))
                MarkerManager.removeMarker(markerSet, markerID)
            }

            val marker = try {
                build.build()
            } catch (e: Exception) {
                sendBuildError(sender, "/$setupCommandPrefix")
                e.printStackTrace()
                return
            }
            if (marker == null) {
                sendRequiredError(sender, "/$setupCommandPrefix")
                return
            }
            if (MarkerManager.addMarker(markerSet, marker, markerID)) {
                builder.remove(id)
                sender.sendMessage(prefix + locale.msg("command.createdMarker"))
            } else sender.sendMessage(prefix + cmp("The marker set ", cError) + cmp(markerSet, cError, underlined = true) + cmp(" does not exist (/$mainCommandPrefix set-create)", cError))
        }
    }

    fun buildSet(sender: Audience, id: String) {
        if (!builderSet.contains(id)) noBuilder(sender, true)
        else {
            val build = builderSet[id]
            val mapName = build?.getArgs()?.get(MarkerArg.MAP)?.getString()
            val setID = build?.getArgs()?.get(MarkerArg.ID)?.getString()
            if (setID == null || mapName == null) {
                sender.sendMessage(prefix + locale.msg("command.mustProvideIDSet"))
                return
            }
            if (!validateID(setID)) {
                sender.sendMessage(prefix + locale.msg("command.mustAlphanumeric"))
                return
            }
            if (MarkerManager.getAllSetIDs(mapName).contains(setID)) {
                sender.sendMessage(prefix + locale.msg("command.idAlreadyExist", listOf(id)))
                return
            }

            val markerSet = try {
                build.buildMarkerSet()
            } catch (e: Exception) {
                sendBuildError(sender, "/$setupSetCommandPrefix")
                e.printStackTrace()
                return
            }
            if (markerSet == null) {
                sendRequiredError(sender, "/$setupSetCommandPrefix")
                return
            }

            if (!MarkerManager.addSet(setID, mapName, markerSet)) {
                sender.sendMessage(prefix + cmp("Something went wrong... Check if BlueMap is already loaded or contact support", cError))
                return
            }
            builderSet.remove(id)

            sender.sendMessage(
                prefix +
                        locale.msg("command.createdSet") +
                        cmp("/$mainCommandPrefix create", cMark, underlined = true).addSuggest("/$mainCommandPrefix create ").addHover(cmp("${locale.msgUse()} /$mainCommandPrefix create <type>"))
            )
        }
    }

    fun cancel(sender: Audience, id: String, isSet: Boolean = false) {
        val removed = if (isSet) builderSet.remove(id) else builder.remove(id)
        if (removed == null) noBuilder(sender, isSet)
        else sender.sendMessage(prefix + locale.msg("command.canceledSetup", listOf(if (isSet) "-set" else "")))
    }

    fun edit(sender: Audience, id: String, setID: String?, markerID: String?) {
        if (builder.contains(id)) {
            sender.sendMessage(alreadyStarted)
            return
        }
        if (setID == null || markerID == null) {
            invalidData(sender, setID, markerID)
            return
        }
        val marker = MarkerManager.getMarker(setID, markerID)
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
        val markerType = enumOf<MarkerType>(marker.type.uppercase())
        if (markerType == null) {
            sender.sendMessage(prefix + cmp("Could not resolve marker type ", cError) + cmp(marker.type, cError, underlined = true) + cmp("! Outdated?", cError))
            return
        }
        val newBuilder = MarkerBuilder.ofMarker(marker, markerType, markerID, setID)
        if (newBuilder == null) {
            sender.sendMessage(prefix + cmp("Something unexpected went wrong while reading marker data... Please contact support", cError))
            return
        }
        builder[id] = newBuilder
        sender.sendMessage(
            prefix + cmp("Editing marker ") +
                    cmp(markerID, cMark) + cmp(". Changing the ") +
                    cmp("ID", cMark) + cmp(" or ") +
                    cmp("Set", cMark) + cmp(" will clone this marker!")
        )
        sendStatusInfo(sender, id)
    }

    fun migrateMarkers(sender: Audience) {
        BlueMapAPI.getInstance().ifPresentOrElse({ api ->
            val info = prefix + cmp("Start migrating internal markers to BMM...")
            consoleAudience.sendMessage(info)
            sender.sendMessage(info)
            api.maps.forEach { map ->
                val name = map.name
                val bmmMarkers = MarkerManager.getAllSetIDs()
                val markers = map.markerSets.filter { !bmmMarkers.contains(it.key) }
                if (markers.isEmpty()) {
                    consoleAudience.sendMessage(prefix + cmp(" -> Skip Empty Map: $name"))
                    return@forEach
                }
                consoleAudience.sendMessage(prefix + cmp(" -> Start Map: $name (${markers.size})"))
                markers.forEach { (id, set) ->
                    if (MarkerManager.addSet(id ?: UUID.randomUUID().toString(), name ?: "Unknown", set)) {
                        consoleAudience.sendMessage(prefix + cmp("  - Set $id successfully migrated", cSuccess))
                    } else consoleAudience.sendMessage(prefix + cmp("  - Set $id failed to migrate! Reason above", cError))
                }
                consoleAudience.sendMessage(prefix + cmp(" -> Finished Map: $name"))
            }

            val infoEnd = prefix + cmp("Finished to migrate internal markers to BMM! You can edit them now via command and remove them from the BlueMap config", cSuccess)
            consoleAudience.sendMessage(infoEnd)
            sender.sendMessage(infoEnd)
        }) {
            sender.sendMessage(prefix + cmp("Failed to connect to BlueMap! Are you using the latest version?", cError))
        }
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
                    MarkerArg.ADD_POSITION -> builder.getVec3List()
                    MarkerArg.ADD_EDGE -> builder.getVec2List()
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

    fun sendBuildError(sender: Audience, cmd: String) {
        sender.sendMessage(
            prefix +
                    cmp("An unexpected error occurred! Please validate your arguments with ", cError) +
                    cmp(cmd, cError, underlined = true).addCommand(cmd).addHover(cmp(cmd)) +
                    cmp(" or report it to the BlueMap Discord (#3rd-party-support)")
        )
    }

    fun sendRequiredError(sender: Audience, cmd: String) {
        sender.sendMessage(
            prefix +
                    cmp("A required option is not set! Type ", cError) +
                    cmp(cmd, cError, underlined = true).addCommand(cmd).addHover(cmp(cmd)) +
                    cmp(" to see more information", cError)
        )
    }

    fun setMarkerArgument(sender: Audience, id: String, type: MarkerArg, value: Any?, message: String, isSet: Boolean = false) {
        if (value == null) {
            sender.playSound(Sound.sound(Key.key("entity.enderman.teleport"), Sound.Source.MASTER, 1f, 1f))
            sender.sendMessage(prefix + cmp("Please enter any value!", cError))
            return
        }
        val builder = getBuilder(sender, id, isSet) ?: return
        builder.setArg(type, ArgumentValue(value))
        sendAppliedSuccess(sender, id, message, isSet)
    }

    fun addMarkerArgumentList(sender: Audience, id: String, type: MarkerArg, value: Any, message: String, isSet: Boolean = false) {
        val builder = getBuilder(sender, id, isSet) ?: return
        when (value) {
            is Vector3d -> builder.getVec3List().add(value)
            is Vector2d -> builder.getVec2List().add(value)
        }
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