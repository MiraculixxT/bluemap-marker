package de.miraculixx.bmm

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector2i
import com.flowpowered.math.vector.Vector3d
import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import de.bluecolored.bluemap.api.math.Color
import de.miraculixx.bmm.map.MarkerBuilder
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.map.MarkerSetBuilder
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.message.*
import de.miraculixx.kpaper.commands.*
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.coordinates.Vec2Argument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault

@Suppress("unused")
class MarkerCommand : MarkerCommandInstance {
    override val builder: MutableMap<String, MarkerBuilder> = mutableMapOf()
    override val builderSet: MutableMap<String, MarkerSetBuilder> = mutableMapOf()

    val mainCommand = command(mainCommandPrefix) {
        requiresPermission(Permission("bmarker.command.main", PermissionDefault.OP))
        // /marker create <type>
        literal("create") {
            requiresPermission(Permission("bmarker.command.create", PermissionDefault.OP))
            argument<String>("type", StringArgumentType.word()) {
                suggestList { listOf("poi", "line", "shape", "extrude") }
                runs {
                    create(sender.bukkitSender, sender.textName, getArgument("type"))
                }
            }
        }

        // /marker delete <map> <set-id> <marker-id>
        literal("delete") {
            requiresPermission(Permission("bmarker.command.delete", PermissionDefault.OP))
            argument<String>("map", StringArgumentType.word()) {
                suggestList { MarkerManager.getAllMaps() }
                argument<String>("marker-set", StringArgumentType.word()) {
                    suggestList { ctx -> MarkerManager.getAllSetIDs(ctx.getArgument("map")) }
                    argument<String>("marker-id", StringArgumentType.word()) {
                        suggestList { ctx -> MarkerManager.getAllMarkers("${ctx.getArgument<String>("marker-set")}_${ctx.getArgument<String>("map")}").keys }
                        runs {
                            delete(sender.bukkitSender, getArgument("map"), getArgument("marker-set"), getArgument("marker-id"))
                        }
                    }
                }
            }
        }

        // /marker edit <map> <set-id> <marker-id>
        literal("edit") {
            requiresPermission(Permission("bmarker.command.edit", PermissionDefault.OP))
            argument<String>("map", StringArgumentType.word()) {
                suggestList { MarkerManager.getAllMaps() }
                argument<String>("marker-set", StringArgumentType.word()) {
                    suggestList { ctx -> MarkerManager.getAllSetIDs(ctx.getArgument("map")) }
                    argument<String>("marker-id", StringArgumentType.word()) {
                        suggestList { ctx -> MarkerManager.getAllMarkers("${ctx.getArgument<String>("marker-set")}_${ctx.getArgument<String>("map")}").keys }
                        runs {
                            val markerSet = "${getArgument<String>("marker-set")}_${getArgument<String>("map")}"
                            edit(sender.bukkitSender, sender.textName, markerSet, getArgument("marker-id"))
                        }
                    }
                }
            }
        }

        // /marker set-create
        literal("set-create") {
            requiresPermission(Permission("bmarker.command.set-create", PermissionDefault.OP))
            runs {
                createSet(sender.bukkitSender, sender.textName)
            }
        }

        // /marker set-delete <map> <id> <true>
        literal("set-delete") {
            requiresPermission(Permission("bmarker.command.set-delete", PermissionDefault.OP))
            argument<String>("map", StringArgumentType.word()) {
                suggestList { MarkerManager.getAllMaps() }
                argument<String>("set-id", StringArgumentType.word()) {
                    suggestList { ctx -> MarkerManager.getAllSetIDs(ctx.getArgument("map")) }
                    runs {
                        confirmDelete(sender.bukkitSender, getArgument("set-id"), getArgument("map"))
                    }
                    argument<Boolean>("confirm", BoolArgumentType.bool()) {
                        runs {
                            deleteSet(sender.bukkitSender, getArgument("confirm"), getArgument("set-id"), getArgument("map"))
                        }
                    }
                }
            }
        }
    }

    val setupMarkerCommand = command(setupCommandPrefix) {
        requiresPermission(Permission("bmarker.command.create", PermissionDefault.OP))

        runs {
            sendStatusInfo(sender.bukkitSender, sender.textName)
        }

        // SETUP COMMANDS
        literal("build") {
            runs {
                build(sender.bukkitSender, sender.textName)
            }
        }
        literal("cancel") {
            runs {
                cancel(sender.bukkitSender, sender.textName)
            }
        }

        // Single String / URL
        literal("icon") {
            argument<String>("icon", StringArgumentType.greedyString()) {
                runs {
                    val value = getArgument<String>("icon")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.ICON, value, "icon URL $value")
                }
            }
        }
        literal("link") {
            argument<String>("link", StringArgumentType.greedyString()) {
                runs {
                    val value = getArgument<String>("link")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.LINK, value, "icon URL $value")
                }
            }
        }
        idLogic(false)

        // Multi Strings
        labelLogic(false)
        literal("detail") {
            argument<String>("detail", StringArgumentType.greedyString()) {
                runs {
                    val value = getArgument<String>("detail")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.DETAIL, value, "detail $value")
                }
            }
        }

        // Dimensions / Worlds
        literal("marker_set") {
            argument<String>("marker-set", StringArgumentType.word()) {
                suggestList { MarkerManager.getAllSetIDs() }
                runs {
                    val value = getArgument<String>("marker-set")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.MARKER_SET, value, "marker-set $value")
                }
            }
        }

        // Locations
        literal("position") {
            argument<Coordinates>("position", Vec3Argument(true)) {
                runs {
                    val position = getArgument<Coordinates>("position").getPosition(sender)
                    val value = Vector3d(position.x.round(2), position.y.round(2), position.z.round(2))
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.POSITION, value, "position $value")
                }
            }
        }
        literal("anchor") {
            argument<Coordinates>("anchor", Vec2Argument(true)) {
                runs {
                    val anchor = getArgument<Coordinates>("anchor").getPosition(sender)
                    val value = Vector2i(anchor.x, anchor.z)
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.ANCHOR, value, "anchor $value")
                }
            }
        }
        literal("add_position") {
            argument<Coordinates>("add-direction", Vec3Argument(true)) {
                runs {
                    val newDirection = getArgument<Coordinates>("add-direction").getPosition(sender)
                    val value = Vector3d(newDirection.x.round(2), newDirection.y.round(2), newDirection.z.round(2))
                    addMarkerArgumentList(sender.bukkitSender, sender.textName, MarkerArg.ADD_POSITION, value, "new direction $value")
                }
            }
        }
        literal("add_edge") {
            argument<Coordinates>("add-edge", Vec2Argument(true)) {
                runs {
                    val edge = getArgument<Coordinates>("add-edge").getPosition(sender)
                    val value = Vector2d(edge.x.round(2), edge.z.round(2))
                    addMarkerArgumentList(sender.bukkitSender, sender.textName, MarkerArg.ADD_EDGE, value, "new edge $value")
                }
            }
        }

        // Doubles
        literal("max_distance") {
            argument<Double>("max-distance", DoubleArgumentType.doubleArg(0.0)) {
                runs {
                    val value = getArgument<Double>("max-distance")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.MAX_DISTANCE, value, "maximal distance $value")
                }
            }
        }
        literal("min_distance") {
            argument<Double>("min-distance", DoubleArgumentType.doubleArg(0.0)) {
                runs {
                    val value = getArgument<Double>("min-distance")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.MIN_DISTANCE, value, "minimal distance $value")
                }
            }
        }

        // Integer
        literal("line_width") {
            argument<Int>("line-width", IntegerArgumentType.integer(0)) {
                runs {
                    val value = getArgument<Int>("line-width")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.LINE_WIDTH, value, "line width $value")
                }
            }
        }

        // Floats
        literal("height") {
            argument<Float>("height", FloatArgumentType.floatArg()) {
                runs {
                    val value = getArgument<Float>("height")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.HEIGHT, value, "height $value")
                }
            }
        }
        literal("max_height") {
            argument<Float>("max-height", FloatArgumentType.floatArg()) {
                runs {
                    val value = getArgument<Float>("max-height")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.MAX_HEIGHT, value, "maximal height $value")
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
            argument<Boolean>("new-tab", BoolArgumentType.bool()) {
                runs {
                    val value = getArgument<Boolean>("new-tab")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.NEW_TAB, value, "open new tab on click $value")
                }
            }
        }
        literal("depth_test") {
            argument<Boolean>("depth-test", BoolArgumentType.bool()) {
                runs {
                    val value = getArgument<Boolean>("depth-test")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.DEPTH_TEST, value, "depth test $value")
                }
            }
        }
    }

    val setupSetCommand = command(setupSetCommandPrefix) {
        requiresPermission(Permission("bmarker.command.set-create", PermissionDefault.OP))

        runs {
            sendStatusInfo(sender.bukkitSender, sender.textName, true)
        }

        // SETUP COMMANDS
        literal("build") {
            runs {
                buildSet(sender.bukkitSender, sender.textName)
            }
        }
        literal("cancel") {
            runs {
                cancel(sender.bukkitSender, sender.textName, true)
            }
        }

        // Worlds
        literal("map") {
            argument<String>("map", StringArgumentType.word()) {
                suggestList { MarkerManager.getAllMaps() }
                runs {
                    val value = getArgument<String>("map").replace(' ','.')
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.MAP, value, "map $value", true)
                }
            }
        }

        // Booleans
        literal("toggleable") {
            argument<Boolean>("toggleable", BoolArgumentType.bool()) {
                runs {
                    val value = getArgument<Boolean>("toggleable")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.TOGGLEABLE, value, "toggleable $value", true)
                }
            }
        }
        literal("default_hidden") {
            argument<Boolean>("default-hidden", BoolArgumentType.bool()) {
                runs {
                    val value = getArgument<Boolean>("default-hidden")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.DEFAULT_HIDDEN, value, "default hidden $value", true)
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
    private fun LiteralArgumentBuilder<CommandSourceStack>.colorLogic(arg: MarkerArg): ArgumentBuilder<CommandSourceStack, *> {
        return argument<Int>("color-r", IntegerArgumentType.integer(0, 255)) {
            argument<Int>("color-g", IntegerArgumentType.integer(0, 255)) {
                argument<Int>("color-b", IntegerArgumentType.integer(0, 255)) {
                    argument<Float>("opacity", FloatArgumentType.floatArg(0f, 1f)) {
                        runs {
                            val colorR = getArgument<Int>("color-r")
                            val colorG = getArgument<Int>("color-g")
                            val colorB = getArgument<Int>("color-b")
                            val opacity = getArgument<Float>("opacity")
                            val value = Color(colorR, colorG, colorB, opacity)
                            setMarkerArgument(sender.bukkitSender, sender.textName, arg, value, "color ${value.stringify()}")
                        }
                    }
                }
            }
        }
    }

    private fun ArgumentBuilder<CommandSourceStack, *>.labelLogic(isSet: Boolean): LiteralArgumentBuilder<CommandSourceStack> {
        return literal("label") {
            argument<String>("label", StringArgumentType.greedyString()) {
                runs {
                    val value = getArgument<String>("label")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.LABEL, value, "label $value", isSet)
                }
            }
        }
    }

    private fun ArgumentBuilder<CommandSourceStack, *>.idLogic(isSet: Boolean): LiteralArgumentBuilder<CommandSourceStack> {
        return literal("id") {
            argument<String>("id", StringArgumentType.word()) {
                runs {
                    val value = getArgument<String>("id")
                    setMarkerArgument(sender.bukkitSender, sender.textName, MarkerArg.ID, value, "ID $value", isSet)
                }
            }
        }
    }
}
