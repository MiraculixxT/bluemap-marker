@file:Suppress("UNCHECKED_CAST")

package de.miraculixx.bmm

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector2i
import com.flowpowered.math.vector.Vector3d
import de.bluecolored.bluemap.api.math.Color
import de.miraculixx.bmm.map.MarkerBuilder
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.map.MarkerSetBuilder
import de.miraculixx.bmm.map.data.Box
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.kpaper.extensions.worlds
import de.miraculixx.mcommons.extensions.round
import de.miraculixx.mcommons.text.cMark
import de.miraculixx.mcommons.text.cmp
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import dev.jorel.commandapi.wrappers.Location2D
import io.papermc.paper.adventure.AdventureComponent
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

@Suppress("unused")
class MarkerCommand : MarkerCommandInstance {
    override val builder: MutableMap<String, MarkerBuilder> = mutableMapOf()
    override val builderSet: MutableMap<String, MarkerSetBuilder> = mutableMapOf()

    val mainCommand = commandTree(mainCommandPrefix) {
        withPermission("bmarker.command.main")

        // /marker create <type> [<map>] [<marker-set>]
        literalArgument("create") {
            withPermission("bmarker.command.create")
            textArgument("type") {
                replaceSuggestions(ArgumentSuggestions.strings(listOf("poi", "line", "shape", "extrude", "ellipse")))
                anyExecutor { sender, args ->
                    create(sender, sender.name, args[0].toString(), null, null, worlds.map { it.name })
                }
                textArgument("map") {
                    suggestMapIDs()
                    anyExecutor { sender, args ->
                        create(sender, sender.name, args[0].toString(), args[1].toString(), null, null)
                    }
                    textArgument("marker-set") {
                        suggestSetIDs()
                        anyExecutor { sender, args ->
                            create(sender, sender.name, args[0].toString(), args[1].toString(), args[2].toString(), null)
                        }
                    }
                }
            }
        }

        // /marker delete [<map>] [<set-id>] [<marker-id>]
        literalArgument("delete") {
            withPermission("bmarker.command.delete")
            anyExecutor { sender, _ ->
                delete(sender, null, null, null, worlds.map { it.name })
            }
            textArgument("map") {
                suggestMapIDs()
                anyExecutor { sender, args ->
                    delete(sender, args[0].toString(), null, null, null)
                }
                textArgument("marker-set") {
                    suggestSetIDs()
                    anyExecutor { sender, args ->
                        delete(sender, args[0].toString(), args[1].toString(), null, null)
                    }
                    textArgument("marker") {
                        suggestMarkerIDs()
                        anyExecutor { sender, args ->
                            delete(sender, args[0].toString(), args[1].toString(), args[2].toString(), null)
                        }
                    }
                }
            }
        }

        // /marker edit [<map>] [<set-id>] [<marker-id>]
        literalArgument("edit") {
            withPermission("bmarker.command.edit")
            anyExecutor { sender, _ ->
                edit(sender, sender.name, null, null, null, worlds.map { it.name })
            }
            textArgument("map") {
                suggestMapIDs()
                anyExecutor { sender, args ->
                    edit(sender, sender.name, args[0].toString(), null, null, null)
                }
                textArgument("marker-set") {
                    suggestSetIDs()
                    anyExecutor { sender, args ->
                        edit(sender, sender.name, args[0].toString(), args[1].toString(), null, null)
                    }
                    textArgument("marker") {
                        suggestMarkerIDs()
                        anyExecutor { sender, args ->
                            edit(sender, sender.name, args[0].toString(), args[1].toString(), args[2].toString(), null)
                        }
                    }
                }
            }
        }

        // /marker set-create [<map-id>]
        literalArgument("set-create") {
            withPermission("bmarker.command.set-create")
            anyExecutor { sender, _ ->
                createSet(sender, sender.name, null, worlds.map { it.name })
            }
            textArgument("map") {
                suggestMapIDs()
                anyExecutor { sender, args ->
                    val value = args[0].toString()
                    createSet(sender, sender.name, value, null)
                }
            }
        }

        // /marker set-delete [<map>] [<id>] [true]
        literalArgument("set-delete") {
            withPermission("bmarker.command.set-delete")
            anyExecutor { sender, _ ->
                deleteSet(sender, false, null, null, worlds.map { it.name })
            }
            textArgument("map") {
                suggestMapIDs()
                anyExecutor { sender, args ->
                    deleteSet(sender, false, null, args[0].toString(), null)
                }
                textArgument("set-id") {
                    suggestSetIDs()
                    anyExecutor { sender, args ->
                        deleteSet(sender, false, args[1].toString(), args[0].toString(), null)
                    }
                    booleanArgument("confirm") {
                        anyExecutor { sender, args ->
                            deleteSet(sender, args[2] == true, args[1].toString(), args[0].toString(), null)
                        }
                    }
                }
            }
        }

        // /marker language TODO
//        literalArgument("language") {
//            withPermission("bmarker.command.language")
//            stringArgument("key") {
//                replaceSuggestions(ArgumentSuggestions.stringCollection { getLanguageKeys() })
//                anyExecutor { sender, args ->
//                    changeLanguage(sender, args[0] as String)
//                }
//            }
//        }
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
                    val value = args[0] as String
                    setMarkerArgument(sender, sender.name, MarkerArg.ICON, Box.BoxString(value), "icon URL $value")
                }
            }
        }
        literalArgument("link") {
            greedyStringArgument("link") {
                anyExecutor { sender, args ->
                    val value = args[0] as String
                    setMarkerArgument(sender, sender.name, MarkerArg.LINK, Box.BoxString(value), "icon URL $value")
                }
            }
        }
        idLogic(false)

        // Multi Strings
        labelLogic(false)
        literalArgument("detail") {
            greedyStringArgument("detail") {
                anyExecutor { sender, args ->
                    val value = args[0] as String
                    setMarkerArgument(sender, sender.name, MarkerArg.DETAIL, Box.BoxString(value), "detail $value")
                }
            }
        }

        // Locations
        literalArgument("position") {
            locationArgument("position", LocationType.PRECISE_POSITION) {
                anyExecutor { sender, args ->
                    val position = args[0] as Location
                    val value = Vector3d(position.x.round(2), position.y.round(2), position.z.round(2))
                    setMarkerArgument(sender, sender.name, MarkerArg.POSITION, Box.BoxVector3d(value), "position $value")
                }
            }
        }
        literalArgument("anchor") {
            location2DArgument("anchor", LocationType.PRECISE_POSITION) {
                anyExecutor { sender, args ->
                    val anchor = args[0] as Location2D
                    val value = Vector2i(anchor.x, anchor.z)
                    setMarkerArgument(sender, sender.name, MarkerArg.ANCHOR, Box.BoxVector2i(value), "anchor $value")
                }
            }
        }
        literalArgument("add_position") {
            literalArgument("remove-last") {
                anyExecutor { sender, _ ->
                    val box = getMarkerArgument(sender, sender.name, MarkerArg.ADD_POSITION) as? Box.BoxVector3dList ?: return@anyExecutor
                    box.value.dropLast(1)
                    setMarkerArgument(sender, sender.name, MarkerArg.ADD_POSITION, box, "removed last position")
                }
            }
            locationArgument("add-position", LocationType.PRECISE_POSITION) {
                anyExecutor { sender, args ->
                    val newDirection = args[0] as Location
                    val value = Vector3d(newDirection.x.round(2), newDirection.y.round(2), newDirection.z.round(2))
                    val box = getMarkerArgument(sender, sender.name, MarkerArg.ADD_POSITION) as? Box.BoxVector3dList ?: return@anyExecutor
                    box.value.add(value)
                    setMarkerArgument(sender, sender.name, MarkerArg.ADD_POSITION, box, "new direction $value")
                }
            }
        }
        literalArgument("add_edge") {
            literalArgument("remove-last") {
                anyExecutor { sender, _ ->
                    val box = getMarkerArgument(sender, sender.name, MarkerArg.ADD_EDGE) as? Box.BoxVector2dList ?: return@anyExecutor
                    box.value.dropLast(1)
                    setMarkerArgument(sender, sender.name, MarkerArg.ADD_EDGE, box, "removed last edge")
                }
            }
            location2DArgument("anchor", LocationType.PRECISE_POSITION) {
                anyExecutor { sender, args ->
                    val edge = args[0] as Location2D
                    val value = Vector2d(edge.x.round(2), edge.z.round(2))
                    val box = getMarkerArgument(sender, sender.name, MarkerArg.ADD_EDGE) as? Box.BoxVector2dList ?: return@anyExecutor
                    box.value.add(value)
                    setMarkerArgument(sender, sender.name, MarkerArg.ADD_EDGE, box, "new edge $value")
                }
            }
        }

        // Doubles
        literalArgument("max_distance") {
            doubleArgument("max-distance", 0.0) {
                anyExecutor { sender, args ->
                    val value = args[0] as Double
                    setMarkerArgument(sender, sender.name, MarkerArg.MAX_DISTANCE, Box.BoxDouble(value), "maximal distance $value")
                }
            }
        }
        literalArgument("min_distance") {
            doubleArgument("min-distance", 0.0) {
                anyExecutor { sender, args ->
                    val value = args[0] as Double
                    setMarkerArgument(sender, sender.name, MarkerArg.MIN_DISTANCE, Box.BoxDouble(value), "minimal distance $value")
                }
            }
        }
        literalArgument("x_radius") {
            doubleArgument("x-radius", 1.0) {
                anyExecutor { sender, args ->
                    val value = args[0] as Double
                    setMarkerArgument(sender, sender.name, MarkerArg.X_RADIUS, Box.BoxDouble(value), "x radius $value")
                }
            }
        }
        literalArgument("z_radius") {
            doubleArgument("z-radius", 1.0) {
                anyExecutor { sender, args ->
                    val value = args[0] as Double
                    setMarkerArgument(sender, sender.name, MarkerArg.Z_RADIUS, Box.BoxDouble(value), "z radius $value")
                }
            }
        }

        // Integer
        literalArgument("line_width") {
            integerArgument("line-width", 0) {
                anyExecutor { sender, args ->
                    val value = args[0] as Int
                    setMarkerArgument(sender, sender.name, MarkerArg.LINE_WIDTH, Box.BoxInt(value), "line width $value")
                }
            }
        }
        literalArgument("points") {
            integerArgument("points", 5) {
                anyExecutor { sender, args ->
                    val value = args[0] as Int
                    setMarkerArgument(sender, sender.name, MarkerArg.POINTS, Box.BoxInt(value), "ellipse points $value")
                }
            }
        }

        // Floats
        literalArgument("height") {
            floatArgument("height") {
                anyExecutor { sender, args ->
                    val value = args[0] as Float
                    setMarkerArgument(sender, sender.name, MarkerArg.HEIGHT, Box.BoxFloat(value), "height $value")
                }
            }
        }
        literalArgument("max_height") {
            floatArgument("max-height") {
                anyExecutor { sender, args ->
                    val value = args[0] as Float
                    setMarkerArgument(sender, sender.name, MarkerArg.MAX_HEIGHT, Box.BoxFloat(value), "maximal height $value")
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
                    val value = args[0] as Boolean
                    setMarkerArgument(sender, sender.name, MarkerArg.NEW_TAB, Box.BoxBoolean(value), "open new tab on click $value")
                }
            }
        }
        literalArgument("depth_test") {
            booleanArgument("depth-test") {
                anyExecutor { sender, args ->
                    val value = args[0] as Boolean
                    setMarkerArgument(sender, sender.name, MarkerArg.DEPTH_TEST, Box.BoxBoolean(value), "depth test $value")
                }
            }
        }
        literalArgument("listed") {
            booleanArgument("listed") {
                anyExecutor { sender, args ->
                    val value = args[0] as Boolean
                    setMarkerArgument(sender, sender.name, MarkerArg.LISTED, Box.BoxBoolean(value), "listing $value")
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

        // Booleans
        literalArgument("toggleable") {
            booleanArgument("toggleable") {
                anyExecutor { sender, args ->
                    val value = args[0] as Boolean
                    setMarkerArgument(sender, sender.name, MarkerArg.TOGGLEABLE, Box.BoxBoolean(value), "toggleable $value", true)
                }
            }
        }
        literalArgument("default_hidden") {
            booleanArgument("default-hidden") {
                anyExecutor { sender, args ->
                    val value = args[0] as Boolean
                    setMarkerArgument(sender, sender.name, MarkerArg.DEFAULT_HIDDEN, Box.BoxBoolean(value), "default hidden $value", true)
                }
            }
        }

        // Integers
        literalArgument("listing_position") {
            integerArgument("position", 0) {
                anyExecutor { sender, args ->
                    val value = args[0] as Int
                    setMarkerArgument(sender, sender.name, MarkerArg.LISTING_POSITION, Box.BoxInt(value), "listing position $value", true)
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
                            setMarkerArgument(sender, sender.name, arg, Box.BoxColor(value), "color $value")
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
                    val value = args[0] as String
                    setMarkerArgument(sender, sender.name, MarkerArg.LABEL, Box.BoxString(value), "label $value", isSet)
                }
            }
        }
    }

    private fun CommandTree.idLogic(isSet: Boolean) {
        literalArgument("id") {
            textArgument("id") {
                anyExecutor { sender, args ->
                    val value = args[0] as String
                    setMarkerArgument(sender, sender.name, MarkerArg.ID, Box.BoxString(value), "ID $value", isSet)
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

    private fun <T> Argument<T>.suggestMapIDs() = replaceSuggestions { _, builder ->
        CompletableFuture.supplyAsync {
            val api = MarkerManager.blueMapAPI
            MarkerManager.blueMapMaps.forEach { (mapID, _) ->
                builder.suggest(mapID, AdventureComponent(cmp(api?.getMap(mapID)?.getOrNull()?.name ?: "Unknown", cMark)))
            }
            builder.build()
        }
    }


    private fun <T> Argument<T>.suggestSetIDs() = replaceSuggestions { info, builder ->
        CompletableFuture.supplyAsync {
            val api = MarkerManager.blueMapAPI
            val mapID = info.previousArgs[0].toString()
            val set = MarkerManager.blueMapMaps[mapID]
            set?.forEach { (setID, data) ->
                val mapName = api?.getMap(mapID)?.getOrNull()?.name ?: "Unknown"
                builder.suggest("$setID.$setID", AdventureComponent(cmp("Map: $mapName, Set: ${data.attributes[MarkerArg.LABEL] ?: "Unknown"}", cMark)))
            }
            builder.build()
        }
    }

    private fun <T> Argument<T>.suggestMarkerIDs() = replaceSuggestions { info, builder ->
        CompletableFuture.supplyAsync {
            val mapID = info.previousArgs[0].toString()
            val setID = info.previousArgs[1].toString()
            MarkerManager.blueMapMaps[mapID]?.get(setID)?.markers?.forEach { (id, data) ->
                builder.suggest(id, AdventureComponent(cmp(data.attributes[MarkerArg.LABEL]?.getString() ?: "Unknown", cMark)))
            }
            builder.build()
        }
    }
}
