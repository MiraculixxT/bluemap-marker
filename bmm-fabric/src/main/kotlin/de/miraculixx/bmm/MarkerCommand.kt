package de.miraculixx.bmm

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector2i
import com.flowpowered.math.vector.Vector3d
import com.mojang.brigadier.arguments.*
import de.bluecolored.bluemap.api.math.Color
import de.miraculixx.bmm.map.MarkerBuilder
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.map.MarkerSetBuilder
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.message.round
import de.miraculixx.bmm.utils.message.stringify
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.coordinates.Vec2Argument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.silkmc.silk.commands.ArgumentCommandBuilder
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.commands.command

@Suppress("unused")
class MarkerCommand : MarkerCommandInstance {
    override val builder: MutableMap<String, MarkerBuilder> = mutableMapOf()
    override val builderSet: MutableMap<String, MarkerSetBuilder> = mutableMapOf()

    val mainCommand = command(mainCommandPrefix) {
        requiresPermissionLevel(2)
        // /marker create <type>
        literal("create") {
            requiresPermissionLevel(2)
            argument<String>("type", StringArgumentType.word()) { type ->
                suggestList { listOf("poi", "line", "shape", "extrude") }
                runs {
                    create(source, source.textName, type())
                }
            }
        }

        // /marker delete <map> <set-id> <marker-id>
        literal("delete") {
            requiresPermissionLevel(3)
            argument<String>("map", StringArgumentType.word()) { map ->
                suggestList { MarkerManager.getAllMaps() }
                argument<String>("marker-set", StringArgumentType.word()) { markerSet ->
                    suggestList { ctx -> MarkerManager.getAllSetIDs(ctx.map()) }
                    argument<String>("marker-id", StringArgumentType.word()) { markerID ->
                        suggestList { ctx -> MarkerManager.getAllMarkers("${ctx.markerSet()}_${ctx.map()}").keys }
                        runs {
                            delete(source, map(), markerSet(), markerID())
                        }
                    }
                }
            }
        }

        // /marker edit <map> <set-id> <marker-id>
        literal("edit") {
            requiresPermissionLevel(2)
            argument<String>("map", StringArgumentType.word()) { map ->
                suggestList { MarkerManager.getAllMaps() }
                argument<String>("marker-set", StringArgumentType.word()) { markerSet ->
                    suggestList { ctx -> MarkerManager.getAllSetIDs(ctx.map()) }
                    argument<String>("marker-id", StringArgumentType.word()) { markerID ->
                        suggestList { ctx -> MarkerManager.getAllMarkers("${ctx.markerSet()}_${ctx.map()}").keys }
                        runs {
                            edit(source, source.textName, "${markerSet()}_${map()}", markerID())
                        }
                    }
                }
            }
        }

        // /marker set-create
        literal("set-create") {
            requiresPermissionLevel(4)
            runs {
                createSet(source, source.textName)
            }
        }

        // /marker set-delete <map> <id> <true>
        literal("set-delete") {
            requiresPermissionLevel(4)
            argument<String>("map", StringArgumentType.word()) { map ->
                suggestList { MarkerManager.getAllMaps() }
                argument<String>("set-id", StringArgumentType.word()) { setID ->
                    suggestList { ctx -> MarkerManager.getAllSetIDs(ctx.map()) }
                    runs {
                        confirmDelete(source, setID(), map())
                    }
                    argument<Boolean>("confirm", BoolArgumentType.bool()) { confirm ->
                        runs {
                            deleteSet(source, confirm(), setID(), map())
                        }
                    }
                }
            }
        }
    }

    val setupMarkerCommand = command(setupCommandPrefix) {
        requiresPermissionLevel(2)

        runs {
            sendStatusInfo(source, source.textName)
        }

        // SETUP COMMANDS
        literal("build") {
            runs {
                build(source, source.textName)
            }
        }
        literal("cancel") {
            runs {
                cancel(source, source.textName)
            }
        }

        // Single String / URL
        literal("icon") {
            argument<String>("icon", StringArgumentType.greedyString()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.ICON, value(), "icon URL ${value()}")
                }
            }
        }
        literal("link") {
            argument<String>("link", StringArgumentType.greedyString()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.LINK, value(), "icon URL ${value()}")
                }
            }
        }
        idLogic(false)

        // Multi Strings
        labelLogic(false)
        literal("detail") {
            argument<String>("detail", StringArgumentType.greedyString()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.DETAIL, value(), "detail ${value()}")
                }
            }
        }

        // Dimensions / Worlds
        literal("marker_set") {
            argument<String>("marker-set", StringArgumentType.word()) { value ->
                suggestList { MarkerManager.getAllSetIDs() }
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.MARKER_SET, value(), "marker-set ${value()}")
                }
            }
        }

        // Locations
        literal("position") {
            argument<Coordinates>("position", Vec3Argument(true)) { pos ->
                runs {
                    val position = pos().getPosition(source)
                    val value = Vector3d(position.x.round(2), position.y.round(2), position.z.round(2))
                    setMarkerArgument(source, source.textName, MarkerArg.POSITION, value, "position $value")
                }
            }
        }
        literal("anchor") {
            argument<Coordinates>("anchor", Vec2Argument(true)) { pos ->
                runs {
                    val anchor = pos().getPosition(source)
                    val value = Vector2i(anchor.x, anchor.z)
                    setMarkerArgument(source, source.textName, MarkerArg.ANCHOR, value, "anchor $value")
                }
            }
        }
        literal("add_position") {
            argument<Coordinates>("add-direction", Vec3Argument(true)) { pos ->
                runs {
                    val newDirection = pos().getPosition(source)
                    val value = Vector3d(newDirection.x.round(2), newDirection.y.round(2), newDirection.z.round(2))
                    addMarkerArgumentList(source, source.textName, MarkerArg.ADD_POSITION, value, "new direction $value")
                }
            }
        }
        literal("add_edge") {
            argument<Coordinates>("add-edge", Vec2Argument(true)) { pos ->
                runs {
                    val edge = pos().getPosition(source)
                    val value = Vector2d(edge.x.round(2), edge.z.round(2))
                    addMarkerArgumentList(source, source.textName, MarkerArg.ADD_EDGE, value, "new edge $value")
                }
            }
        }

        // Doubles
        literal("max_distance") {
            argument<Double>("max-distance", DoubleArgumentType.doubleArg(0.0)) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.MAX_DISTANCE, value(), "maximal distance ${value()}")
                }
            }
        }
        literal("min_distance") {
            argument<Double>("min-distance", DoubleArgumentType.doubleArg(0.0)) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.MIN_DISTANCE, value(), "minimal distance ${value()}")
                }
            }
        }

        // Integer
        literal("line_width") {
            argument<Int>("line-width", IntegerArgumentType.integer(0)) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.LINE_WIDTH, value(), "line width ${value()}")
                }
            }
        }

        // Floats
        literal("height") {
            argument<Float>("height", FloatArgumentType.floatArg()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.HEIGHT, value(), "height ${value()}")
                }
            }
        }
        literal("max_height") {
            argument<Float>("max-height", FloatArgumentType.floatArg()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.MAX_HEIGHT, value(), "maximal height ${value()}")
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
                    setMarkerArgument(source, source.textName, MarkerArg.NEW_TAB, value(), "open new tab on click ${value()}")
                }
            }
        }
        literal("depth_test") {
            argument<Boolean>("depth-test", BoolArgumentType.bool()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.DEPTH_TEST, value(), "depth test ${value()}")
                }
            }
        }
    }

    val setupSetCommand = command(setupSetCommandPrefix) {
        requiresPermissionLevel(3)

        runs {
            sendStatusInfo(source, source.textName, true)
        }

        // SETUP COMMANDS
        literal("build") {
            runs {
                buildSet(source, source.textName)
            }
        }
        literal("cancel") {
            runs {
                cancel(source, source.textName, true)
            }
        }

        // Worlds
        literal("map") {
            argument<String>("map", StringArgumentType.word()) { value ->
                suggestList { MarkerManager.getAllMaps() }
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.MAP, value(), "map ${value()}", true)
                }
            }
        }

        // Booleans
        literal("toggleable") {
            argument<Boolean>("toggleable", BoolArgumentType.bool()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.TOGGLEABLE, value(), "toggleable ${value()}", true)
                }
            }
        }
        literal("default_hidden") {
            argument<Boolean>("default-hidden", BoolArgumentType.bool()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.DEFAULT_HIDDEN, value(), "default hidden ${value()}", true)
                }
            }
        }

        // String / Multi Strings
        labelLogic(true)
        idLogic(true)
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
                            setMarkerArgument(source, source.textName, arg, value, "color ${value.stringify()}")
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
                    setMarkerArgument(source, source.textName, MarkerArg.LABEL, value(), "label ${value()}", isSet)
                }
            }
        }
    }

    private fun LiteralCommandBuilder<CommandSourceStack>.idLogic(isSet: Boolean): LiteralCommandBuilder<CommandSourceStack> {
        return literal("id") {
            argument<String>("id", StringArgumentType.word()) { value ->
                runs {
                    setMarkerArgument(source, source.textName, MarkerArg.ID, value(), "ID ${value()}", isSet)
                }
            }
        }
    }
}
