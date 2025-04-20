package de.miraculixx.bmm.commands

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector2i
import com.flowpowered.math.vector.Vector3d
import com.mojang.brigadier.arguments.*
import de.bluecolored.bluemap.api.math.Color
import de.miraculixx.bmm.map.data.Box
import de.miraculixx.bmm.utils.data.*
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.mcommons.extensions.round
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.GameProfileArgument
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.coordinates.Vec2Argument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.silkmc.silk.commands.ArgumentCommandBuilder
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.commands.command
import de.miraculixx.bmm.commands.CommandHelper.suggestMapIDs
import de.miraculixx.bmm.commands.CommandHelper.suggestMarkerIDs
import de.miraculixx.bmm.commands.CommandHelper.suggestSetIDs

@Suppress("unused")
class MarkerCommand : MarkerCommandInstance {

    val mainCommand = command(mainCommandPrefix) {
        requires {
            Permissions.require(manageOwnMarkers, 2).test(it)
        }
        // /marker create <type>
        literal("create") {
            argument<String>("type", StringArgumentType.word()) { type ->
                suggestList { listOf("poi", "line", "shape", "extrude", "ellipse") }
                runs {
                    create(source, source.textName, type(), null, null, source.getWorldKeys(), null)
                }
                argument<String>("map", StringArgumentType.word()) { map ->
                    suggestMapIDs()
                    runsAsync {
                        create(source, source.textName, type(), map(), null, null, source.getData())
                    }
                    argument<String>("marker-set", StringArgumentType.word()) { markerSet ->
                        suggestSetIDs("map")
                        runsAsync {
                            create(source, source.textName, type(), map(), markerSet(), null, source.getData())
                        }
                    }
                }
            }
        }

        // /marker delete <map> <set-id> <marker-id>
        literal("delete") {
            runsAsync {
                delete(source, null, null, null, source.getWorldKeys(), null)
            }
            argument<String>("map", StringArgumentType.word()) { map ->
                suggestMapIDs()
                runsAsync {
                    delete(source, map(), null, null, null, source.getData())
                }
                argument<String>("marker-set", StringArgumentType.word()) { markerSet ->
                    suggestSetIDs("map")
                    runsAsync {
                        delete(source, map(), markerSet(), null, null, source.getData())
                    }
                    argument<String>("marker-id", StringArgumentType.word()) { markerID ->
                        suggestMarkerIDs("map", "marker-set")
                        runsAsync {
                            delete(source, map(), markerSet(), markerID(), null, source.getData())
                        }
                    }
                }
            }
        }

        // /marker edit <map> <set-id> <marker-id>
        literal("edit") {
            runsAsync {
                edit(source, source.textName, null, null, null, source.getWorldKeys(), null)
            }
            argument<String>("map", StringArgumentType.word()) { map ->
                suggestMapIDs()
                runsAsync {
                    edit(source, source.textName, map(), null, null, null, source.getData())
                }
                argument<String>("marker-set", StringArgumentType.word()) { markerSet ->
                    suggestSetIDs("map")
                    runsAsync {
                        edit(source, source.textName, map(), markerSet(), null, null, source.getData())
                    }
                    argument<String>("marker-id", StringArgumentType.word()) { markerID ->
                        suggestMarkerIDs("map", "marker-set")
                        runsAsync {
                            edit(source, source.textName, map(), markerSet(), markerID(), null, source.getData())
                        }
                    }
                }
            }
        }

        // /marker set-create
        literal("set-create") {
            requires {
                Permissions.require(manageOwnSets, 4).test(it)
            }
            runsAsync {
                createSet(source, source.textName, null, source.getWorldKeys(), null)
            }
            argument<String>("map", StringArgumentType.word()) { map ->
                suggestMapIDs()
                runsAsync {
                    createSet(source, source.textName, map(), null, source.getData())
                }
            }
        }

        // /marker set-delete <map> <id> <true>
        literal("set-delete") {
            requires {
                Permissions.require(manageOwnSets, 4).test(it)
            }
            runsAsync {
                deleteSet(source, false, null, null, source.getWorldKeys(), null)
            }
            argument<String>("map", StringArgumentType.word()) { map ->
                suggestMapIDs()
                runsAsync {
                    deleteSet(source, false, null, map(), null, source.getData())
                }
                argument<String>("id", StringArgumentType.word()) { id ->
                    suggestSetIDs("map")
                    runsAsync {
                        deleteSet(source, false, id(), map(), null, source.getData())
                    }
                    argument<Boolean>("confirm", BoolArgumentType.bool()) { confirm ->
                        runsAsync {
                            deleteSet(source, confirm(), id(), map(), null, source.getData())
                        }
                    }
                }
            }
        }

        // /marker set-edit <map> <id>
        literal("set-edit") {
            runsAsync {
                editSet(source, source.textName, null, null, source.getWorldKeys(), null)
            }
            argument<String>("map", StringArgumentType.word()) { map ->
                suggestMapIDs()
                runsAsync {
                    editSet(source, source.textName, map(), null, null, source.getData())
                }
                argument<String>("id", StringArgumentType.word()) { id ->
                    suggestSetIDs("map")
                    runsAsync {
                        editSet(source, source.textName, map(), id(), null, source.getData())
                    }
                }
            }
        }
    }

    val setupMarkerCommand = command(setupCommandPrefix) {
        requires {
            Permissions.require(manageOwnMarkers, 2).test(it)
        }

        runs {
            sendStatusInfo(source, source.textName, isConsole = !source.isPlayer)
        }

        // SETUP COMMANDS
        literal("build") {
            runs {
                build(source, source.textName, source.player?.uuid)
            }
        }
        literal("cancel") {
            runs {
                cancel(source, source.textName)
            }
        }
        pageLogic(false)

        // Single String / URL
        literal("icon") {
            argument<String>("icon", StringArgumentType.greedyString()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.ICON, Box.BoxString(value()), "icon URL ${value()}")
                }
            }
        }
        literal("link") {
            argument<String>("link", StringArgumentType.greedyString()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.LINK, Box.BoxString(value()), "icon URL ${value()}")
                }
            }
        }
        idLogic(false)

        // Multi Strings
        labelLogic(false)
        literal("detail") {
            argument<String>("detail", StringArgumentType.greedyString()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.DETAIL, Box.BoxString(value()), "detail ${value()}")
                }
            }
        }

        // Locations
        literal("position") {
            argument<Coordinates>("position", Vec3Argument(true)) { pos ->
                runs {
                    val position = pos().getPosition(source)
                    val value = Vector3d(position.x.round(2), position.y.round(2), position.z.round(2))
                    setMarkerArgument(source, source.textName, MarkerArg.POSITION, Box.BoxVector3d(value), "position $value")
                }
            }
        }
        literal("anchor") {
            argument<Coordinates>("anchor", Vec2Argument(true)) { pos ->
                runs {
                    val anchor = pos().getPosition(source)
                    val value = Vector2i(anchor.x, anchor.z)
                    setMarkerArgument(source, source.textName, MarkerArg.ANCHOR, Box.BoxVector2i(value), "anchor $value")
                }
            }
        }
        literal("add_position") {
            literal("remove-last") {
                runsAsync {
                    val box = getMarkerArgument(source, source.textName, MarkerArg.ADD_POSITION) as? Box.BoxVector3dList ?: Box.BoxVector3dList(mutableListOf())
                    box.value.removeLast()
                    setMarkerArgument(source, source.textName, MarkerArg.ADD_POSITION, box, "removed last position")
                }
            }
            argument<Coordinates>("add-position", Vec3Argument(true)) { pos ->
                runs {
                    val newDirection = pos().getPosition(source)
                    val value = Vector3d(newDirection.x.round(2), newDirection.y.round(2), newDirection.z.round(2))
                    val box = getMarkerArgument(source, source.textName, MarkerArg.ADD_POSITION) as? Box.BoxVector3dList ?: Box.BoxVector3dList(mutableListOf())
                    box.value.add(value)
                    setMarkerArgument(source, source.textName, MarkerArg.ADD_POSITION, box, "new direction $value")
                }
            }
        }
        literal("add_edge") {
            literal("remove-last") {
                runsAsync {
                    val box = getMarkerArgument(source, source.textName, MarkerArg.ADD_EDGE) as? Box.BoxVector2dList ?: Box.BoxVector2dList(mutableListOf())
                    box.value.removeLast()
                    setMarkerArgument(source, source.textName, MarkerArg.ADD_EDGE, box, "removed last edge")
                }
            }
            argument<Coordinates>("add-edge", Vec2Argument(true)) { pos ->
                runs {
                    val edge = pos().getPosition(source)
                    val value = Vector2d(edge.x.round(2), edge.z.round(2))
                    val box = getMarkerArgument(source, source.textName, MarkerArg.ADD_EDGE) as? Box.BoxVector2dList ?: Box.BoxVector2dList(mutableListOf())
                    box.value.add(value)
                    setMarkerArgument(source, source.textName, MarkerArg.ADD_EDGE, box, "new edge $value")
                }
            }
        }

        // Doubles
        literal("max_distance") {
            argument<Double>("max-distance", DoubleArgumentType.doubleArg(0.0)) { value ->
                runs {
                    setMarkerArgument(
                        source, source.textName, MarkerArg.MAX_DISTANCE, Box.BoxDouble(value()), "maximal distance ${value()}"
                    )
                }
            }
        }
        literal("min_distance") {
            argument<Double>("min-distance", DoubleArgumentType.doubleArg(0.0)) { value ->
                runs {
                    setMarkerArgument(
                        source, source.textName, MarkerArg.MIN_DISTANCE, Box.BoxDouble(value()), "minimal distance ${value()}"
                    )
                }
            }
        }
        literal("x_radius") {
            argument<Double>("x-radius", DoubleArgumentType.doubleArg(1.0)) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.X_RADIUS, Box.BoxDouble(value()), "x radius ${value()}")
                }
            }
        }
        literal("z_radius") {
            argument<Double>("z-radius", DoubleArgumentType.doubleArg(1.0)) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.Z_RADIUS, Box.BoxDouble(value()), "z radius ${value()}")
                }
            }
        }

        // Integer
        literal("line_width") {
            argument<Int>("line-width", IntegerArgumentType.integer(0)) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.LINE_WIDTH, Box.BoxInt(value()), "line width ${value()}")
                }
            }
        }
        literal("points") {
            argument<Int>("points", IntegerArgumentType.integer(5)) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.POINTS, Box.BoxInt(value()), "ellipse points ${value()}")
                }
            }
        }
        listingLogic(false)

        // Floats
        literal("height") {
            argument<Float>("height", FloatArgumentType.floatArg()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.HEIGHT, Box.BoxFloat(value()), "height ${value()}")
                }
            }
        }
        literal("max_height") {
            argument<Float>("max-height", FloatArgumentType.floatArg()) { value ->
                runs {
                    setMarkerArgument(
                        source, source.textName, MarkerArg.MAX_HEIGHT, Box.BoxFloat(value()), "maximal height ${value()}"
                    )
                }
            }
        }

        // Color
        literal("line_color") {
            colorLogic(MarkerArg.LINE_COLOR)
        }
        literal("fill_color") {
            colorLogic(MarkerArg.FILL_COLOR)
        }

        // Booleans
        literal("new_tab") {
            argument<Boolean>("new-tab", BoolArgumentType.bool()) { value ->
                runs {
                    setMarkerArgument(
                        source, source.textName, MarkerArg.NEW_TAB, Box.BoxBoolean(value()), "open new tab on click ${value()}"
                    )
                }
            }
        }
        literal("depth_test") {
            argument<Boolean>("depth-test", BoolArgumentType.bool()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.DEPTH_TEST, Box.BoxBoolean(value()), "depth test ${value()}")
                }
            }
        }
        literal("listed") {
            argument<Boolean>("listed", BoolArgumentType.bool()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.LISTED, Box.BoxBoolean(value()), "listing ${value()}")
                }
            }
        }
    }

    val setupSetCommand = command(setupSetCommandPrefix) {
        requires {
            Permissions.require(manageOwnSets, 3).test(it)
        }

        runs {
            sendStatusInfo(source, source.textName, true, isConsole = !source.isPlayer)
        }

        // SETUP COMMANDS
        literal("build") {
            runs {
                buildSet(source, source.textName, source.player?.uuid)
            }
        }
        literal("cancel") {
            runs {
                cancel(source, source.textName, true)
            }
        }
        pageLogic(true)

        // Booleans
        literal("toggleable") {
            argument<Boolean>("toggleable", BoolArgumentType.bool()) { value ->
                runs {
                    setMarkerArgument(
                        source, source.textName, MarkerArg.TOGGLEABLE, Box.BoxBoolean(value()), "toggleable ${value()}", true
                    )
                }
            }
        }
        literal("default_hidden") {
            argument<Boolean>("default-hidden", BoolArgumentType.bool()) { value ->
                runs {
                    setMarkerArgument(
                        source, source.textName, MarkerArg.DEFAULT_HIDDEN, Box.BoxBoolean(value()), "default hidden ${value()}", true
                    )
                }
            }
        }

        // Integers
        listingLogic(true)

        // String / Multi Strings
        labelLogic(true)
        idLogic(true)
    }

    val visibilityCommand = command(visibilityCommandPrefix) {
        requires {
            Permissions.require(changeVisibility, 3).test(it)
        }

        literal("hide") {
            visibility(false)
        }
        literal("show") {
            visibility(true)
        }
    }


    /*
     *
     * Extensions to prevent duplication
     *
     */
    private fun LiteralCommandBuilder<CommandSourceStack>.colorLogic(arg: MarkerArg): ArgumentCommandBuilder<CommandSourceStack, *> {
        return argument<Int>("color-r", IntegerArgumentType.integer(0, 255)) { r ->
            argument<Int>("color-g", IntegerArgumentType.integer(0, 255)) { g ->
                argument<Int>("color-b", IntegerArgumentType.integer(0, 255)) { b ->
                    argument<Float>("opacity", FloatArgumentType.floatArg(0f, 1f)) { o ->
                        runs {
                            val value = Color(r(), g(), b(), o())
                            setMarkerArgument(source, source.textName, arg, Box.BoxColor(value), "color $value")
                        }
                    }
                }
            }
        }
    }

    private fun LiteralCommandBuilder<CommandSourceStack>.labelLogic(isSet: Boolean): LiteralCommandBuilder<CommandSourceStack> {
        return literal("label") {
            argument<String>("label", StringArgumentType.greedyString()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.LABEL, Box.BoxString(value()), "label ${value()}", isSet)
                }
            }
        }
    }

    private fun LiteralCommandBuilder<CommandSourceStack>.idLogic(isSet: Boolean): LiteralCommandBuilder<CommandSourceStack> {
        return literal("id") {
            argument<String>("id", StringArgumentType.word()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.ID, Box.BoxString(value()), "ID ${value()}", isSet)
                }
            }
        }
    }

    private fun LiteralCommandBuilder<CommandSourceStack>.visibility(visible: Boolean) {
        runs {
            val player = source.player ?: return@runs
            setPlayerVisibility(source, listOf(player.uuid to player.scoreboardName), visible)
        }

        argument<GameProfileArgument.Result>("target", GameProfileArgument.gameProfile()) { target ->
            requires {
                Permissions.require(changeVisibilityOthers, 3).test(it)
            }
            runs {
                val profiles = target().getNames(source).map { it.id to it.name }
                setPlayerVisibility(source, profiles, visible)
            }
        }
    }

    private fun LiteralCommandBuilder<CommandSourceStack>.pageLogic(isSet: Boolean) {
        literal("page") {
            literal("next") {
                runsAsync {
                    getBuilder(source, source.textName, isSet)?.let { it.page++ }
                    sendStatusInfo(source, source.textName, isSet, isConsole = !source.isPlayer)
                }
            }
            literal("previous") {
                runsAsync {
                    getBuilder(source, source.textName, isSet)?.let { it.page-- }
                    sendStatusInfo(source, source.textName, isSet, isConsole = !source.isPlayer)
                }
            }
        }
    }

    private fun LiteralCommandBuilder<CommandSourceStack>.listingLogic(isSet: Boolean) {
        literal("listing_position") {
            argument<Int>("listing-position", IntegerArgumentType.integer(Int.MIN_VALUE)) { value ->
                runsAsync {
                    setMarkerArgument(source, source.textName, MarkerArg.LISTING_POSITION, Box.BoxInt(value()), "listing position $value", isSet)
                }
            }
        }
    }

    private fun CommandSourceStack.getData(): PlayerData {
        val isOP = player?.permissionLevel?.let { it >= 3 } == true
        return PlayerData(player?.uuid, textName, isOP || Permissions.require(manageOthersMarkers).test(this), isOP || Permissions.require(manageOthersSets).test(this))
    }

    private fun CommandSourceStack.getWorldKeys(): List<String> {
        return server.allLevels.map { it.dimension().key().value() }
    }
}
