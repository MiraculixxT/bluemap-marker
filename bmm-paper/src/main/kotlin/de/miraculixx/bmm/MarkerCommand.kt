@file:Suppress("UNCHECKED_CAST")

package de.miraculixx.bmm

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector2i
import com.flowpowered.math.vector.Vector3d
import de.bluecolored.bluemap.api.math.Color
import de.miraculixx.bmm.map.MarkerBuilder
import de.miraculixx.bmm.map.MarkerSetBuilder
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.message.round
import de.miraculixx.bmm.utils.message.stringify
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.*
import dev.jorel.commandapi.kotlindsl.*
import dev.jorel.commandapi.wrappers.Location2D
import org.bukkit.Location
import org.bukkit.entity.Player

@Suppress("unused")
class MarkerCommand : MarkerCommandInstance {
    override val builder: MutableMap<String, MarkerBuilder> = mutableMapOf()
    override val builderSet: MutableMap<String, MarkerSetBuilder> = mutableMapOf()

    val mainCommand = commandTree(mainCommandPrefix) {
        withPermission("bmarker.command.main")

        // /marker create <type>
        argument(LiteralArgument("create").withPermission("bmarker.command.create")) {
            argument(TextArgument("type").replaceSuggestions(ArgumentSuggestions.strings(listOf("poi", "line", "shape", "extrude", "ellipse")))) {
                anyExecutor { sender, args ->
                    create(sender, sender.name, args[0].toString())
                }
            }
        }

        // /marker delete <map> <set-id> <marker-id>
        argument(LiteralArgument("delete").withPermission("bmarker.command.delete")) {
            argument(TextArgument("map").replaceSuggestions(ArgumentSuggestions.stringCollection { MarkerManager.getAllMaps() })) {
                argument(TextArgument("marker-set").replaceSuggestions(ArgumentSuggestions.stringCollection { MarkerManager.getAllSetIDs(it.previousArgs[0].toString()) })) {
                    argument(TextArgument("marker-id").replaceSuggestions(ArgumentSuggestions.stringCollection {
                        val args = it.previousArgs
                        MarkerManager.getAllMarkers("${args[1]}_${args[0]}").keys
                    })) {
                        anyExecutor { sender, args ->
                            delete(sender, args[0].toString(), args[1].toString(), args[2].toString())
                        }
                    }
                }
            }
        }

        // /marker edit <map> <set-id> <marker-id>
        argument(LiteralArgument("edit").withPermission("bmarker.command.edit")) {
            argument(TextArgument("map").replaceSuggestions(ArgumentSuggestions.stringCollection { MarkerManager.getAllMaps() })) {
                argument(TextArgument("marker-set").replaceSuggestions(ArgumentSuggestions.stringCollection { MarkerManager.getAllSetIDs(it.previousArgs[0].toString()) })) {
                    argument(TextArgument("marker-id").replaceSuggestions(ArgumentSuggestions.stringCollection {
                        val args = it.previousArgs
                        MarkerManager.getAllMarkers("${args[1]}_${args[0]}").keys
                    })) {
                        anyExecutor { sender, args ->
                            val markerSet = "${args[1]}_${args[0]}"
                            edit(sender, sender.name, markerSet, args[2].toString())
                        }
                    }
                }
            }
        }

        // /marker set-create
        argument(LiteralArgument("set-create").withPermission("bmarker.command.set-create")) {
            anyExecutor { sender, _ ->
                createSet(sender, sender.name)
            }
        }

        // /marker set-delete <map> <id> <true>
        argument(LiteralArgument("set-delete").withPermission("bmarker.command.set-delete")) {
            argument(TextArgument("map").replaceSuggestions(ArgumentSuggestions.stringCollection { MarkerManager.getAllMaps() })) {
                argument(TextArgument("set-id").replaceSuggestions(ArgumentSuggestions.stringCollection { MarkerManager.getAllSetIDs(it.previousArgs[0].toString()) })) {
                    anyExecutor { sender, args ->
                        confirmDelete(sender, args[1].toString(), args[0].toString())
                    }
                    booleanArgument("confirm") {
                        anyExecutor { sender, args ->
                            deleteSet(sender, args[2] == true, args[1].toString(), args[0].toString())
                        }
                    }
                }
            }
        }

        // /marker migrate
        argument(LiteralArgument("migrate").withPermission("bmarker.command.migrate")) {
            anyExecutor { sender, _ ->
                migrateMarkers(sender)
            }
            argument(TextArgument("map").replaceSuggestions(ArgumentSuggestions.stringCollection { MarkerManager.getAllMaps() })) {
                textArgument("set-id") {
                    greedyStringArgument("input") {
                        consoleExecutor { sender, args ->
                            migrateMarkers(sender, args[2].toString(), args[1].toString(), args[0].toString())
                        }
                    }
                }
            }
        }

        // /marker language
        literalArgument("language") {
            withPermission("bmarker.command.language")
            stringArgument("key") {
                replaceSuggestions(ArgumentSuggestions.stringCollection { getLanguageKeys() })
                anyExecutor { sender, args ->
                    changeLanguage(sender, args[0] as String)
                }
            }
        }
    }

    val setupMarkerCommand = commandTree(setupCommandPrefix) {
        withPermission("bmarker.command.create")

        anyExecutor { sender, _ ->
            sendStatusInfo(sender, sender.name)
        }

        // SETUP COMMANDS
        literalArgument("build") {
            anyExecutor { sender, _ ->
                build(sender, sender.name)
            }
        }
        literalArgument("cancel") {
            anyExecutor { sender, _ ->
                cancel(sender, sender.name)
            }
        }

        // Single String / URL
        literalArgument("icon") {
            greedyStringArgument("icon") {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.ICON, value, "icon URL $value")
                }
            }
        }
        literalArgument("link") {
            greedyStringArgument("link") {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.LINK, value, "icon URL $value")
                }
            }
        }
        idLogic(false)

        // Multi Strings
        labelLogic(false)
        literalArgument("detail") {
            greedyStringArgument("detail") {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.DETAIL, value, "detail $value")
                }
            }
        }

        // Dimensions / Worlds
        literalArgument("marker_set") {
            argument(TextArgument("marker-set").replaceSuggestions(ArgumentSuggestions.stringCollection { MarkerManager.getAllSetIDs() })) {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.MARKER_SET, value, "marker-set $value")
                }
            }
        }

        // Locations
        literalArgument("position") {
            locationArgument("position", LocationType.PRECISE_POSITION) {
                anyExecutor { sender, args ->
                    val position = args[0] as Location
                    val value = Vector3d(position.x.round(2), position.y.round(2), position.z.round(2))
                    setMarkerArgument(sender, sender.name, MarkerArg.POSITION, value, "position $value")
                }
            }
        }
        literalArgument("anchor") {
            location2DArgument("anchor", LocationType.PRECISE_POSITION) {
                anyExecutor { sender, args ->
                    val anchor = args[0] as Location2D
                    val value = Vector2i(anchor.x, anchor.z)
                    setMarkerArgument(sender, sender.name, MarkerArg.ANCHOR, value, "anchor $value")
                }
            }
        }
        literalArgument("add_position") {
            locationArgument("add-position", LocationType.PRECISE_POSITION) {
                anyExecutor { sender, args ->
                    val newDirection = args[0] as Location
                    val value = Vector3d(newDirection.x.round(2), newDirection.y.round(2), newDirection.z.round(2))
                    addMarkerArgumentList(sender, sender.name, MarkerArg.ADD_POSITION, value, "new direction $value")
                }
            }
        }
        literalArgument("add_edge") {
            location2DArgument("anchor", LocationType.PRECISE_POSITION) {
                anyExecutor { sender, args ->
                    val edge = args[0] as Location2D
                    val value = Vector2d(edge.x.round(2), edge.z.round(2))
                    addMarkerArgumentList(sender, sender.name, MarkerArg.ADD_EDGE, value, "new edge $value")
                }
            }
        }

        // Doubles
        literalArgument("max_distance") {
            doubleArgument("max-distance", 0.0) {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.MAX_DISTANCE, value, "maximal distance $value")
                }
            }
        }
        literalArgument("min_distance") {
            doubleArgument("min-distance", 0.0) {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.MIN_DISTANCE, value, "minimal distance $value")
                }
            }
        }
        literalArgument("x_radius") {
            doubleArgument("x-radius", 1.0) {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.X_RADIUS, value, "x radius $value")
                }
            }
        }
        literalArgument("z_radius") {
            doubleArgument("z-radius", 1.0) {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.Z_RADIUS, value, "z radius $value")
                }
            }
        }

        // Integer
        literalArgument("line_width") {
            integerArgument("line-width", 0) {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.LINE_WIDTH, value, "line width $value")
                }
            }
        }
        literalArgument("points") {
            integerArgument("points", 5) {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.POINTS, value, "ellipse points $value")
                }
            }
        }

        // Floats
        literalArgument("height") {
            floatArgument("height") {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.HEIGHT, value, "height $value")
                }
            }
        }
        literalArgument("max_height") {
            floatArgument("max-height") {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.MAX_HEIGHT, value, "maximal height $value")
                }
            }
        }

        // Color
        literalArgument("line_color") {
            colorLogic(MarkerArg.LINE_COLOR)
        }
        literalArgument("fill_color") {
            colorLogic(MarkerArg.FILL_COLOR)
        }

        // Booleans
        literalArgument("new_tab") {
            booleanArgument("new-tab") {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.NEW_TAB, value, "open new tab on click $value")
                }
            }
        }
        literalArgument("depth_test") {
            booleanArgument("depth-test") {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.DEPTH_TEST, value, "depth test $value")
                }
            }
        }
        literalArgument("listed") {
            booleanArgument("listed") {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.LISTED, value, "listing $value")
                }
            }
        }
    }

    val setupSetCommand = commandTree(setupSetCommandPrefix) {
        withPermission("bmarker.command.set-create")

        anyExecutor { sender, _ ->
            sendStatusInfo(sender, sender.name, true)
        }

        // SETUP COMMANDS
        literalArgument("build") {
            anyExecutor { sender, _ ->
                buildSet(sender, sender.name)
            }
        }
        literalArgument("cancel") {
            anyExecutor { sender, _ ->
                cancel(sender, sender.name, true)
            }
        }

        // Worlds
        literalArgument("map") {
            argument(TextArgument("map").replaceSuggestions(ArgumentSuggestions.stringCollection { MarkerManager.getAllMaps() })) {
                anyExecutor { sender, args ->
                    val value = args[0].toString().replace(' ', '.')
                    setMarkerArgument(sender, sender.name, MarkerArg.MAP, value, "map $value", true)
                }
            }
        }

        // Booleans
        literalArgument("toggleable") {
            booleanArgument("toggleable") {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.TOGGLEABLE, value, "toggleable $value", true)
                }
            }
        }
        literalArgument("default_hidden") {
            booleanArgument("default-hidden") {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.DEFAULT_HIDDEN, value, "default hidden $value", true)
                }
            }
        }

        // String / Multi Strings
        labelLogic(true)
        idLogic(true)
    }

    val visibilityCommand = commandTree(visibilityCommandPrefix) {
        withPermission("bmarker.command.visibility")

        literalArgument("hide") {
            visibility(false)
        }
        literalArgument("show") {
            visibility(true)
        }
    }


    /*
     *
     * Extensions to prevent duplication
     *
     */
    private fun Argument<*>.colorLogic(arg: MarkerArg) {
        integerArgument("color-r", 0, 255) {
            integerArgument("color-g", 0, 255) {
                integerArgument("color-b", 0, 255) {
                    floatArgument("opacity", 0f, 1f) {
                        anyExecutor { sender, args ->
                            val colorR = args[0].toString().toIntOrNull() ?: 0
                            val colorG = args[1].toString().toIntOrNull() ?: 0
                            val colorB = args[2].toString().toIntOrNull() ?: 0
                            val opacity = args[3].toString().toFloatOrNull() ?: 1f
                            val value = Color(colorR, colorG, colorB, opacity)
                            setMarkerArgument(sender, sender.name, arg, value, "color ${value.stringify()}")
                        }
                    }
                }
            }
        }
    }

    private fun CommandTree.labelLogic(isSet: Boolean) {
        literalArgument("label") {
            greedyStringArgument("label") {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.LABEL, value, "label $value", isSet)
                }
            }
        }
    }

    private fun CommandTree.idLogic(isSet: Boolean) {
        literalArgument("id") {
            textArgument("id") {
                anyExecutor { sender, args ->
                    val value = args[0]
                    setMarkerArgument(sender, sender.name, MarkerArg.ID, value, "ID $value", isSet)
                }
            }
        }
    }

    private fun Argument<*>.visibility(visible: Boolean) {
        playerExecutor { player, _ ->
            setPlayerVisibility(player, listOf(player.uniqueId to player.name), visible)
        }
        entitySelectorArgumentManyPlayers("target") {
            withPermission("bmarker.command.visibility-other")
            anyExecutor { sender, args ->
                val profiles = (args[0] as Collection<Player>).map { it.uniqueId to it.name }
                setPlayerVisibility(sender, profiles, visible)
            }
        }
    }
}
