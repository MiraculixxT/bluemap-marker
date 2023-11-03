package de.miraculixx.bmm

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector3d
import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.miraculixx.bmm.map.MarkerBuilder
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.map.MarkerSetBuilder
import de.miraculixx.bmm.map.data.ArgumentValue
import de.miraculixx.bmm.map.interfaces.Builder
import de.miraculixx.bmm.utils.enumOf
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.enums.MarkerType
import de.miraculixx.bmm.utils.message.*
import de.miraculixx.bmm.utils.settings
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.util.UUID

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
        get() = prefix + msg("command.alreadyStarted") +
                cmp(msgCancel, cError, underlined = true).addClick("/$setupCommandPrefix cancel", true) +
                cmp(" $msgOr ", cError) +
                cmp(msgBuild, cError, underlined = true).addClick("/$setupCommandPrefix build", true) +
                msg("command.alreadyStarted")

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
                sender.sendMessage(prefix + msg("command.notValidMarker", listOf(type ?: "Unknown")))
                return
            }
            builder[id] = MarkerBuilder(markerType)
            sender.sendMessage(
                prefix +
                        cmp("Marker setup started! Modify values using ") +
                        cmp("/$setupCommandPrefix", cMark, underlined = true).addSuggest("/$setupCommandPrefix ").addHover(cmp("Use /$setupCommandPrefix <arg> <value>")) +
                        cmp(" and finish your setup with ") +
                        cmp("/$setupCommandPrefix build", cMark, underlined = true).addClick("/$setupCommandPrefix build", true)
            )
            sendStatusInfo(sender, id)
        }
    }

    fun createSet(sender: Audience, id: String) {
        if (builderSet.contains(id)) {
            sender.sendMessage(
                prefix +
                        msg("command.alreadyStarted") +
                        cmp(msgCancel, cError, underlined = true).addClick("/$setupSetCommandPrefix cancel", true) +
                        cmp(" $msgOr ", cError) +
                        cmp(msgBuild, cError, underlined = true).addClick("/$setupSetCommandPrefix build", true) +
                        msg("command.alreadyStarted")
            )
        } else {
            builderSet[id] = MarkerSetBuilder()
            sender.sendMessage(
                prefix +
                        cmp("Marker-Set setup started! Modify values using ") +
                        cmp("/$setupSetCommandPrefix", cMark, underlined = true).addSuggest("/$setupSetCommandPrefix ").addHover(cmp("Use /$setupSetCommandPrefix <arg> <value>")) +
                        cmp(" and finish your setup with ") +
                        cmp("/$setupSetCommandPrefix build", cMark, underlined = true).addClick("/$setupSetCommandPrefix build", true)
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
            sender.sendMessage(prefix + cmp("Successfully deleted ") + cmp(markerID, cMark) + cmp(" marker! It should disappear from your BlueMap in a few seconds"))
        } else sender.sendMessage(prefix + cmp("This marker does not exist!", cError))
    }

    fun confirmDelete(sender: Audience, setID: String, mapName: String) {
        sender.sendMessage(
            prefix +
                    cmp("Are you really sure you want to delete the '$setID' set on map '$mapName'? Please confirm by typing ", cError) +
                    cmp("/$mainCommandPrefix set-delete $mapName $setID true", cError, underlined = true)
        )
    }

    fun deleteSet(sender: Audience, confirm: Boolean, setID: String?, mapName: String?) {
        if (!confirm) return
        if (setID == null || mapName == null) {
            sender.sendMessage(prefix + cmp("Invalid set-ID or map name! ($setID - $mapName)", cError))
            return
        }
        if (MarkerManager.removeSet(setID, mapName)) {
            sender.sendMessage(prefix + cmp("Successfully deleted ") + cmp(setID, cMark) + cmp(" marker-set! It should disappear from your BlueMap in a few seconds"))
        } else sender.sendMessage(prefix + cmp("This marker-set does not exist or BlueMap is not loaded!", cError))
    }

    fun build(sender: Audience, id: String) {
        if (!builder.contains(id)) noBuilder(sender)
        else {
            val build = builder[id]
            val markerSet = build?.getArgs()?.get(MarkerArg.MARKER_SET)?.getString()
            val markerID = build?.getArgs()?.get(MarkerArg.ID)?.getString()
            if (markerSet == null || markerID == null) {
                sender.sendMessage(prefix + cmp("Please provide a marker ID and a target marker-set!", cError))
                return
            }
            if (!validateID(markerID)) {
                sender.sendMessage(prefix + cmp("IDs must be alphanumeric (only contains letters and numbers)", cError))
                return
            }
            if (MarkerManager.getAllMarkers(markerSet).contains(markerID)) {
                sender.sendMessage(prefix + cmp("The ID ", cError) + cmp(markerID, cError, underlined = true) + cmp(" already exist in this set!", cError))
                sender.sendMessage(prefix + cmp("The old marker will be replaced with your new one..."))
                MarkerManager.removeMarker(markerSet, markerID)
            }

            val marker = try {
                build.buildMarker()
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
                sender.sendMessage(prefix + cmp("Marker created! It should appear on your BlueMap in a few seconds"))
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
                sender.sendMessage(prefix + cmp("Please provide a marker-set ID and a target world!", cError))
                return
            }
            if (!validateID(setID)) {
                sender.sendMessage(prefix + cmp("IDs must be alphanumeric (only contains letters and numbers)", cError))
                return
            }
            if (MarkerManager.getAllSetIDs(mapName).contains(setID)) {
                sender.sendMessage(prefix + cmp("The ID ", cError) + cmp(setID, cError, underlined = true) + cmp(" already exist in this world!", cError))
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
                        cmp("Marker-Set created! Use it too add new markers inside this set with ") +
                        cmp("/$mainCommandPrefix create", cMark, underlined = true).addSuggest("/$mainCommandPrefix create ").addHover(cmp("/$mainCommandPrefix create <type>"))
            )
        }
    }

    fun cancel(sender: Audience, id: String, isSet: Boolean = false) {
        val removed = if (isSet) builderSet.remove(id) else builder.remove(id)
        if (removed == null) noBuilder(sender, isSet)
        else sender.sendMessage(prefix + cmp("Canceled current marker${if (isSet) "-set" else ""} setup!"))
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
        val newBuilder = MarkerBuilder.of(marker, markerType, markerID, setID)
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

    fun migrateMarkers(sender: Audience, input: String, rawID: String, mapName: String) {
        var final = input.substringAfter('#')
        if (final.startsWith("\"marker-sets\"")) {
            final = final.removePrefix("\"marker-sets\": {").substringBeforeLast('}')
        }
        try {
            val set = gson.fromJson(final, MarkerSet::class.java)
            MarkerManager.addSet(rawID, mapName, set)
            consoleAudience.sendMessage(prefix + cmp("Successfully migrated the marker set!", cSuccess))
        } catch (e: Exception) {
            consoleAudience.sendMessage(prefix + cmp("Failed to convert marker input!", cError))
            consoleAudience.sendMessage(prefix + cmp(e.message ?: "Reason unknown", cError))
        }
    }

    fun setPlayerVisibility(sender: Audience, targets: List<Pair<UUID, String>>, visible: Boolean) {
        BlueMapAPI.getInstance().ifPresentOrElse({ api ->
            if (targets.isEmpty()) {
                sender.sendMessage(prefix + cmp("Could not found given player!", cError))
                return@ifPresentOrElse
            }
            targets.forEach { target ->
                api.webApp.setPlayerVisibility(target.first, visible)
                val info = if (visible) cmp("visible", cSuccess) else cmp("invisible", cError)
                sender.sendMessage(prefix + cmp(target.second, cMark) + cmp(" is now ") + info + cmp(" on your BlueMap!"))
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
        val nothingSet = cmp(msgNotSet, italic = true)
        val dash = cmp("- ")
        val midDash = cmp(" ≫ ", NamedTextColor.DARK_GRAY)
        val cmd = if (isMarkerSet) "/$setupSetCommandPrefix" else "/$setupCommandPrefix"
        val hoverAddition = cmp("\n\n" + msgString("event.clickToAdd"), cMark)
        sender.sendMessage(cmp(" \n") + prefix + cmp(msgString("event.currentSetup", listOf(type.name))))
        type.args.forEach { arg ->
            // List values displayed in a different way than single values
            if (arg == MarkerArg.ADD_POSITION || arg == MarkerArg.ADD_EDGE) {
                val list = when (arg) {
                    MarkerArg.ADD_POSITION -> builder.getVec3List()
                    MarkerArg.ADD_EDGE -> builder.getVec2List()
                    else -> emptyList()
                }
                val isSet = list.isNotEmpty()
                val color = if (!isSet) cError else NamedTextColor.GREEN
                sender.sendMessage(
                    dash +
                            (cmp(msgString("arg.${arg.name}"), color) +
                                    midDash +
                                    if (isSet) cmp("[${list.size} Values]", cMark) else nothingSet)
                                .addSuggest("$cmd ${arg.name.lowercase()} ").addHover(cmp(msgString("arg-desc.${arg.name}")) + hoverAddition)
                )
                return@forEach
            }

            val value = appliedArgs[arg]
            val isSet = value != null
            val color = if (!isSet) if (arg.isRequired) cError else NamedTextColor.GRAY else NamedTextColor.GREEN
            sender.sendMessage(
                dash +
                        (cmp(msgString("arg.${arg.name}"), color) + //arg.name.replace('_', ' ')
                                midDash +
                                if (isSet) cmp(value?.getString() ?: msgNotSet, cMark) else nothingSet)
                            .addSuggest("$cmd ${arg.name.lowercase()} ").addHover(cmp(msgString("arg-desc.${arg.name}")) + hoverAddition)
            )
        }
        sender.sendMessage(
            cmp("                 ", cHighlight, strikethrough = true) +
                    cmp("[ ", cHighlight, strikethrough = false) +
                    cmp(msgBuild.uppercase(), cSuccess, bold = true, strikethrough = false).addClick("$cmd build")
                        .addHover(cmp(msgString("event.buildHover"))) +
                    cmp(" | ") +
                    cmp(msgCancel.uppercase(), cError, bold = true, strikethrough = false).addClick("$cmd cancel").addHover(cmp(msgString("event.cancelHover"))) +
                    cmp(" ]", cHighlight) +
                    cmp("                 ", cHighlight, strikethrough = true)
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
                    cmp(cmd, cError, underlined = true).addClick(cmd, true) +
                    cmp(" or report it to the BlueMap Discord (#3rd-party-support)")
        )
    }

    fun sendRequiredError(sender: Audience, cmd: String) {
        sender.sendMessage(
            prefix +
                    cmp("A required option is not set! Type ", cError) +
                    cmp(cmd, cError, underlined = true).addClick(cmd, true) +
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

    fun changeLanguage(sender: Audience, language: String) {
        if (localization?.setLanguage(language) == true) {
            settings.language = language
            sender.sendMessage(prefix + msg("command.switchLang"))
        } else sender.sendMessage(prefix + msg("command.switchLangFailed"))
    }

    fun getLanguageKeys() = localization?.getLoadedKeys() ?: emptyList()

    private fun invalidData(sender: Audience, setID: String?, markerID: String?) {
        sender.sendMessage(prefix + cmp("Invalid set-ID or marker-ID! ($setID - $markerID)", cError))
    }
}