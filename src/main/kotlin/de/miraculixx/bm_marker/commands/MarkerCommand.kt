package de.miraculixx.bm_marker.commands

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector2i
import com.flowpowered.math.vector.Vector3d
import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import de.bluecolored.bluemap.api.math.Color
import de.miraculixx.bm_marker.map.MarkerBuilder
import de.miraculixx.bm_marker.map.MarkerManager
import de.miraculixx.bm_marker.map.MarkerSetBuilder
import de.miraculixx.bm_marker.map.data.ArgumentValue
import de.miraculixx.bm_marker.map.interfaces.Builder
import de.miraculixx.bm_marker.utils.enumOf
import de.miraculixx.bm_marker.utils.enums.MarkerArg
import de.miraculixx.bm_marker.utils.enums.MarkerType
import de.miraculixx.bm_marker.utils.message.*
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.commands.*
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.DimensionArgument
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.coordinates.Vec2Argument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import org.bukkit.command.CommandSender

@Suppress("unused")
class MarkerCommand {
    private val builder: MutableMap<String, MarkerBuilder> = mutableMapOf()
    private val builderSet: MutableMap<String, MarkerSetBuilder> = mutableMapOf()
    private val mainCommandPrefix = "bmarker"
    private val setupCommandPrefix = "bmarker-setup"
    private val setupSetCommandPrefix = "bmarker-setup-set"

    val mainCommand = command(mainCommandPrefix) {
        // /marker create <type>
        literal("create") {
            argument<String>("type", StringArgumentType.word()) {
                suggestList { listOf("poi", "line", "shape", "extrude") }
                runs {
                    if (builder.contains(sender.textName)) {
                        sender.bukkitSender.sendMessage(prefix + cmp("You already started a marker setup! ", cError) + literalText {
                            component(cmp("Cancel", cError, underlined = true))
                            clickEvent = ClickEvent.runCommand("/$setupCommandPrefix cancel")
                            hoverEvent = HoverEvent.showText(cmp("/$setupCommandPrefix cancel"))
                        } + cmp(" or ", cError) + literalText {
                            component(cmp("build", cError, underlined = true))
                            clickEvent = ClickEvent.runCommand("/$setupCommandPrefix build")
                            hoverEvent = HoverEvent.showText(cmp("/$setupCommandPrefix build"))
                        } + cmp(" it before creating a new one", cError))
                    } else {
                        val type = getArgument<String>("type")

                        val markerType = enumOf<MarkerType>(type.uppercase())
                        if (markerType == null) {
                            sender.bukkitSender.sendMessage(prefix + cmp("This is not a valid marker!", cError))
                            return@runs
                        }
                        builder[sender.textName] = MarkerBuilder(markerType)
                        sender.bukkitSender.sendMessage(prefix + cmp("Marker setup started! Modify values using ") + literalText {
                            component(cmp("/$setupCommandPrefix", cMark, underlined = true))
                            clickEvent = ClickEvent.suggestCommand("/$setupCommandPrefix ")
                            hoverEvent = HoverEvent.showText(cmp("Use /$setupCommandPrefix <arg> <value>"))
                        } + cmp(" and finish your setup with ") + literalText {
                            component(cmp("/$setupCommandPrefix build", cMark, underlined = true))
                            clickEvent = ClickEvent.runCommand("/$setupCommandPrefix build")
                            hoverEvent = HoverEvent.showText(cmp("/$setupCommandPrefix build"))
                        })
                        sendStatusInfo(sender)
                    }
                }
            }
        }

        // /marker delete <world> <set-id> <marker-id>
        literal("delete") {
            argument<ResourceLocation>("world", DimensionArgument()) {
                argument<String>("marker-set", StringArgumentType.word()) {
                    suggestList { ctx -> MarkerManager.getAllSetIDs(ctx.getArgument<ResourceLocation>("world").path) }
                    argument<String>("marker-id", StringArgumentType.word()) {
                        suggestList { ctx -> MarkerManager.getAllMarkers("${ctx.getArgument<String>("marker-set")}_${ctx.getArgument<ResourceLocation>("world").path}").keys }
                        runs {
                            val worldName = getArgument<ResourceLocation>("world").path
                            val setID = getArgument<String>("marker-set")
                            val markerID = getArgument<String>("marker-id")
                            if (MarkerManager.removeMarker("${setID}_$worldName", markerID)) {
                                sender.bukkitSender.sendMessage(prefix + cmp("Successfully deleted ") + cmp(markerID, cMark) + cmp(" marker! It should disappear from your BlueMap in a few seconds"))
                            } else sender.bukkitSender.sendMessage(prefix + cmp("This marker does not exist!", cError))
                        }
                    }
                }
            }
        }

        // /marker create-set
        literal("create-set") {
            runs {
                if (builderSet.contains(sender.textName)) {
                    sender.bukkitSender.sendMessage(prefix + cmp("You already started a marker-set setup! ", cError) + literalText {
                        component(cmp("Cancel", cError, underlined = true))
                        clickEvent = ClickEvent.runCommand("/$setupSetCommandPrefix cancel")
                        hoverEvent = HoverEvent.showText(cmp("/$setupSetCommandPrefix cancel"))
                    } + cmp(" or ", cError) + literalText {
                        component(cmp("build", cError, underlined = true))
                        clickEvent = ClickEvent.runCommand("/$setupSetCommandPrefix build")
                        hoverEvent = HoverEvent.showText(cmp("/$setupSetCommandPrefix build"))
                    } + cmp(" it before creating a new one", cError))
                } else {
                    builderSet[sender.textName] = MarkerSetBuilder()
                    sender.bukkitSender.sendMessage(prefix + cmp("Marker-Set setup started! Modify values using ") + literalText {
                        component(cmp("/$setupSetCommandPrefix", cMark, underlined = true))
                        clickEvent = ClickEvent.suggestCommand("/$setupSetCommandPrefix ")
                        hoverEvent = HoverEvent.showText(cmp("Use /$setupSetCommandPrefix <arg> <value>"))
                    } + cmp(" and finish your setup with ") + literalText {
                        component(cmp("/$setupSetCommandPrefix build", cMark, underlined = true))
                        clickEvent = ClickEvent.runCommand("/$setupSetCommandPrefix build")
                        hoverEvent = HoverEvent.showText(cmp("/$setupSetCommandPrefix build"))
                    })
                    sendStatusInfo(sender, true)
                }
            }
        }

        // /marker delete-set <world> <id> <true>
        literal("delete-set") {
            argument<ResourceLocation>("world", DimensionArgument.dimension()) {
                argument<String>("set-id", StringArgumentType.word()) {
                    suggestList { ctx -> MarkerManager.getAllSetIDs(ctx.getArgument<ResourceLocation>("world").path) }
                    runs {
                        val setID = getArgument<String>("set-id")
                        val worldName = getArgument<ResourceLocation>("world").path
                        sender.bukkitSender.sendMessage(prefix + cmp("Are you really sure you want to delete the '$setID' set in world '$worldName'? Please confirm by typing ", cError) +
                                cmp("/$mainCommandPrefix delete-set $worldName $setID true", cError, underlined = true)
                        )
                    }
                    argument<Boolean>("confirm", BoolArgumentType.bool()) {
                        runs {
                            if (!getArgument<Boolean>("confirm")) return@runs
                            val setID = getArgument<String>("set-id")
                            val worldName = getArgument<ResourceLocation>("world").path

                            if (MarkerManager.removeSet(setID, worldName)) {
                                sender.bukkitSender.sendMessage(prefix + cmp("Successfully deleted ") + cmp(setID, cMark) + cmp(" marker-set! It should disappear from your BlueMap in a few seconds"))
                            } else sender.bukkitSender.sendMessage(prefix + cmp("This marker-set does not exist or BlueMap is not loaded!", cError))
                        }
                    }
                }
            }
        }
    }

    val setupMarkerCommand = command(setupCommandPrefix) {
        runs {
            sendStatusInfo(sender)
        }

        // SETUP COMMANDS
        literal("build") {
            runs {
                if (!builder.contains(sender.textName)) noBuilder(sender)
                else {
                    val build = builder[sender.textName]
                    val markerSet = build?.getArgs()?.get(MarkerArg.MARKER_SET)?.getString()
                    val markerID = build?.getArgs()?.get(MarkerArg.ID)?.getString()
                    val bukkitSender = sender.bukkitSender
                    if (markerSet == null || markerID == null) {
                        bukkitSender.sendMessage(prefix + cmp("Please provide a marker ID and a target marker-set!", cError))
                        return@runs
                    }
                    if (!validateID(markerID)) {
                        bukkitSender.sendMessage(prefix + cmp("IDs must be alphanumeric (only contains letters and numbers)", cError))
                        return@runs
                    }
                    if (MarkerManager.getAllMarkers(markerSet).contains(markerID)) {
                        bukkitSender.sendMessage(prefix + cmp("The ID ", cError) + cmp(markerID, cError, underlined = true) + cmp(" already exist in this set!", cError))
                        return@runs
                    }

                    val marker = try {
                        build.buildMarker()
                    } catch (e: Exception) {
                        sendBuildError(bukkitSender, "/$setupCommandPrefix")
                        e.printStackTrace()
                        return@runs
                    }
                    if (marker == null) {
                        sendRequiredError(bukkitSender, "/$setupCommandPrefix")
                        return@runs
                    }
                    MarkerManager.addMarker(markerSet, marker, markerID)
                    builder.remove(sender.textName)

                    bukkitSender.sendMessage(prefix + cmp("Marker created! It should appear on your BlueMap in a few seconds"))
                }
            }
        }
        literal("cancel") {
            runs {
                if (builder.remove(sender.textName) == null) noBuilder(sender)
                else sender.bukkitSender.sendMessage(prefix + cmp("Canceled current marker setup!"))
            }
        }

        // Single String / URL
        literal("icon") {
            argument<String>("icon", StringArgumentType.greedyString()) {
                runs {
                    val icon = getArgument<String>("icon")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.setArg(MarkerArg.ICON, ArgumentValue(icon))
                    sendAppliedSuccess(sender, "icon URL $icon")
                }
            }
        }
        literal("link") {
            argument<String>("link", StringArgumentType.greedyString()) {
                runs {
                    val link = getArgument<String>("link")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.setArg(MarkerArg.LINK, ArgumentValue(link))
                    sendAppliedSuccess(sender, "link URL $link")
                }
            }
        }
        idLogic(false)

        // Multi Strings
        labelLogic(false)
        literal("detail") {
            argument<String>("detail", StringArgumentType.greedyString()) {
                runs {
                    val detail = getArgument<String>("detail")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.setArg(MarkerArg.DETAIL, ArgumentValue(detail))
                    sendAppliedSuccess(sender, "detail $detail")
                }
            }
        }

        // Dimensions / Worlds
        literal("marker_set") {
            argument<String>("marker-set", StringArgumentType.word()) {
                suggestList { MarkerManager.getAllSetIDs() }
                runs {
                    val markerSet = getArgument<String>("marker-set")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.setArg(MarkerArg.MARKER_SET, ArgumentValue(markerSet))
                    sendAppliedSuccess(sender, "marker-set $markerSet")
                }
            }
        }

        // Locations
        literal("position") {
            argument<Coordinates>("position", Vec3Argument(true)) {
                runs {
                    val position = getArgument<Coordinates>("position").getPosition(sender)
                    val vec3d = Vector3d(position.x.round(2), position.y.round(2), position.z.round(2))
                    val builder = getBuilder(sender) ?: return@runs
                    builder.setArg(MarkerArg.POSITION, ArgumentValue(vec3d))
                    sendAppliedSuccess(sender, "position $vec3d")
                }
            }
        }
        literal("anchor") {
            argument<Coordinates>("anchor", Vec2Argument(true)) {
                runs {
                    val anchor = getArgument<Coordinates>("anchor").getPosition(sender)
                    val vec2d = Vector2i(anchor.x, anchor.z)
                    val builder = getBuilder(sender) ?: return@runs
                    builder.setArg(MarkerArg.ANCHOR, ArgumentValue(anchor))
                    sendAppliedSuccess(sender, "anchor $vec2d")
                }
            }
        }
        literal("add_position") {
            argument<Coordinates>("add-direction", Vec3Argument(true)) {
                runs {
                    val newDirection = getArgument<Coordinates>("add-direction").getPosition(sender)
                    val vec3d = Vector3d(newDirection.x.round(2), newDirection.y.round(2), newDirection.z.round(2))
                    val builder = getBuilder(sender) ?: return@runs
                    builder.getVec3List().add(vec3d)
                    sendAppliedSuccess(sender, "new direction $vec3d")
                }
            }
        }
        literal("add_edge") {
            argument<Coordinates>("add-edge", Vec2Argument(true)) {
                runs {
                    val edge = getArgument<Coordinates>("add-edge").getPosition(sender)
                    val vec2d = Vector2d(edge.x.round(2), edge.z.round(2))
                    val builder = getBuilder(sender) ?: return@runs
                    builder.getVec2List().add(vec2d)
                    sendAppliedSuccess(sender, "new edge $vec2d")
                }
            }
        }

        // Doubles
        literal("max_distance") {
            argument<Double>("max-distance", DoubleArgumentType.doubleArg(0.0)) {
                runs {
                    val maxDistance = getArgument<Double>("max-distance")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.setArg(MarkerArg.MAX_DISTANCE, ArgumentValue(maxDistance))
                    sendAppliedSuccess(sender, "maximal distance $maxDistance")
                }
            }
        }
        literal("min_distance") {
            argument<Double>("min-distance", DoubleArgumentType.doubleArg(0.0)) {
                runs {
                    val minDistance = getArgument<Double>("min-distance")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.setArg(MarkerArg.MIN_DISTANCE, ArgumentValue(minDistance))
                    sendAppliedSuccess(sender, "minimal distance $minDistance")
                }
            }
        }

        // Integer
        literal("line_width") {
            argument<Int>("line-width", IntegerArgumentType.integer(0)) {
                runs {
                    val lineWidth = getArgument<Int>("line-width")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.setArg(MarkerArg.LINE_WIDTH, ArgumentValue(lineWidth))
                    sendAppliedSuccess(sender, "line width $lineWidth")
                }
            }
        }

        // Floats
        literal("height") {
            argument<Float>("height", FloatArgumentType.floatArg()) {
                runs {
                    val height = getArgument<Float>("height")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.setArg(MarkerArg.HEIGHT, ArgumentValue(height))
                    sendAppliedSuccess(sender, "height $height")
                }
            }
        }
        literal("max_height") {
            argument<Float>("max-height", FloatArgumentType.floatArg()) {
                runs {
                    val maxHeight = getArgument<Float>("max-height")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.setArg(MarkerArg.MAX_HEIGHT, ArgumentValue(maxHeight))
                    sendAppliedSuccess(sender, "maximal height $maxHeight")
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
                    val newTab = getArgument<Boolean>("new-tab")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.setArg(MarkerArg.NEW_TAB, ArgumentValue(newTab))
                    sendAppliedSuccess(sender, "open new tab on click $newTab")
                }
            }
        }
        literal("depth_test") {
            argument<Boolean>("depth-test", BoolArgumentType.bool()) {
                runs {
                    val depthTest = getArgument<Boolean>("depth-test")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.setArg(MarkerArg.DEPTH_TEST, ArgumentValue(depthTest))
                    sendAppliedSuccess(sender, "depth test $depthTest")
                }
            }
        }
    }

    val setupSetCommand = command(setupSetCommandPrefix) {
        runs {
            sendStatusInfo(sender, true)
        }

        // SETUP COMMANDS
        literal("build") {
            runs {
                if (!builderSet.contains(sender.textName)) noBuilder(sender)
                else {
                    val build = builderSet[sender.textName]
                    val worldName = build?.getArgs()?.get(MarkerArg.WORLD)?.getString()
                    val setID = build?.getArgs()?.get(MarkerArg.ID)?.getString()
                    val bukkitSender = sender.bukkitSender
                    if (setID == null || worldName == null) {
                        bukkitSender.sendMessage(prefix + cmp("Please provide a marker-set ID and a target world!", cError))
                        return@runs
                    }
                    if (!validateID(setID)) {
                        bukkitSender.sendMessage(prefix + cmp("IDs must be alphanumeric (only contains letters and numbers)", cError))
                        return@runs
                    }
                    if (MarkerManager.getAllSetIDs(worldName).contains(setID)) {
                        bukkitSender.sendMessage(prefix + cmp("The ID ", cError) + cmp(setID, cError, underlined = true) + cmp(" already exist in this world!", cError))
                        return@runs
                    }

                    val markerSet = try {
                        build.buildMarkerSet()
                    } catch (e: Exception) {
                        sendBuildError(bukkitSender, "/$setupSetCommandPrefix")
                        e.printStackTrace()
                        return@runs
                    }
                    if (markerSet == null) {
                        sendRequiredError(bukkitSender, "/$setupSetCommandPrefix")
                        return@runs
                    }

                    if (!MarkerManager.addSet(setID, worldName, markerSet)) {
                        bukkitSender.sendMessage(prefix + cmp("Something went wrong... Check if BlueMap is already loaded or contact support", cError))
                        return@runs
                    }
                    builderSet.remove(sender.textName)

                    bukkitSender.sendMessage(prefix + cmp("Marker-Set created! Use it too add new markers inside this set with ") + literalText("/$mainCommandPrefix create") {
                        color = cMark
                        underline = true
                        clickEvent = ClickEvent.suggestCommand("/$mainCommandPrefix create ")
                        hoverEvent = HoverEvent.showText(cmp("/$mainCommandPrefix create <type>"))
                    })
                }
            }
        }
        literal("cancel") {
            runs {
                if (builderSet.remove(sender.textName) == null) noBuilder(sender, true)
                else sender.bukkitSender.sendMessage(prefix + cmp("Canceled current marker-set setup!"))
            }
        }

        // Worlds
        literal("world") {
            argument<ResourceLocation>("world", DimensionArgument.dimension()) {
                runs {
                    val worldName = getArgument<ResourceLocation>("world").path
                    val builder = getBuilder(sender, true) ?: return@runs
                    builder.setArg(MarkerArg.WORLD, ArgumentValue(worldName))
                    sendAppliedSuccess(sender, "world $worldName", true)
                }
            }
        }

        // Booleans
        literal("toggleable") {
            argument<Boolean>("toggleable", BoolArgumentType.bool()) {
                runs {
                    val toggleable = getArgument<Boolean>("toggleable")
                    val builder = getBuilder(sender, true) ?: return@runs
                    builder.setArg(MarkerArg.TOGGLEABLE, ArgumentValue(toggleable))
                    sendAppliedSuccess(sender, "toggleable $toggleable", true)
                }
            }
        }
        literal("default_hidden") {
            argument<Boolean>("default-hidden", BoolArgumentType.bool()) {
                runs {
                    val defaultHidden = getArgument<Boolean>("default-hidden")
                    val builder = getBuilder(sender, true) ?: return@runs
                    builder.setArg(MarkerArg.DEFAULT_HIDDEN, ArgumentValue(defaultHidden))
                    sendAppliedSuccess(sender, "default hidden $defaultHidden", true)
                }
            }
        }

        // String / Multi Strings
        labelLogic(true)
        idLogic(true)
    }

    private fun noBuilder(sender: CommandSourceStack, isSet: Boolean = false) {
        val addition = if (isSet) "-set" else ""
        sender.bukkitSender.sendMessage(
            prefix + cmp("You have no current marker$addition setups. Start one with ", cError) + literalText {
                component(cmp("/$mainCommandPrefix create$addition", cError, underlined = true))
                clickEvent = ClickEvent.suggestCommand("/$mainCommandPrefix create$addition ")
                hoverEvent = HoverEvent.showText(cmp("Start a marker$addition setup (click)"))
            }
        )
    }

    private fun getBuilder(sender: CommandSourceStack, isSet: Boolean = false): Builder? {
        return if (isSet) {
            builderSet.getOrElse(sender.textName) {
                noBuilder(sender, true)
                return null
            }
        } else {
            builder.getOrElse(sender.textName) {
                noBuilder(sender, true)
                return null
            }
        }
    }

    private fun sendStatusInfo(sender: CommandSourceStack, isMarkerSet: Boolean = false) {
        val builder = getBuilder(sender, isMarkerSet) ?: return
        val bukkitSender = sender.bukkitSender
        val type = builder.getType()
        val appliedArgs = builder.getArgs()
        val nothingSet = cmp("Not Set", italic = true)
        val dash = cmp("- ")
        val midDash = cmp(" ≫ ", NamedTextColor.DARK_GRAY)
        val cmd = if (isMarkerSet) "/$setupSetCommandPrefix" else "/$setupCommandPrefix"
        bukkitSender.sendMessage(cmp(" \n") + prefix + cmp("Your current setup state (${type.name})"))
        type.args.forEach { arg ->
            // List values displayed in a different way than single values
            if (arg == MarkerArg.ADD_POSITION || arg == MarkerArg.ADD_EDGE) {
                bukkitSender.sendMessage(dash + literalText {
                    val list = when (arg) {
                        MarkerArg.ADD_POSITION -> builder.getVec3List()
                        MarkerArg.ADD_EDGE -> builder.getVec2List()
                        else -> emptyList()
                    }
                    val isSet = list.isNotEmpty()
                    val color = if (!isSet) cError else NamedTextColor.GREEN
                    component(
                        cmp(arg.name.replace('_', ' '), color) +
                                midDash +
                                if (isSet) cmp("[${list.size} Values]", cMark) else nothingSet
                    )
                    hoverEvent = HoverEvent.showText(cmp(arg.description) + cmp("\n\nClick to add a value", cMark))
                    clickEvent = ClickEvent.suggestCommand("$cmd ${arg.name.lowercase()} ")
                })
                return@forEach
            }

            bukkitSender.sendMessage(dash + literalText {
                val value = appliedArgs[arg]
                val isSet = value != null
                val color = if (!isSet) if (arg.isRequired) cError else NamedTextColor.GRAY else NamedTextColor.GREEN
                component(
                    cmp(arg.name.replace('_', ' '), color) +
                            midDash +
                            if (isSet) cmp(value?.getString() ?: "Not Set", cMark) else nothingSet
                )
                hoverEvent = HoverEvent.showText(cmp(arg.description) + cmp("\n\nClick to modify value", cMark))
                clickEvent = ClickEvent.suggestCommand("$cmd ${arg.name.lowercase()} ")
            })
        }
        bukkitSender.sendMessage(
            cmp("                 ", cHighlight, strikethrough = true) + cmp("[ ", cHighlight, strikethrough = false) +
                    literalText("BUILD") {
                        color = cSuccess
                        bold = true
                        strikethrough = false
                        clickEvent = ClickEvent.runCommand("$cmd build")
                        hoverEvent = HoverEvent.showText(cmp("Build a new marker with applied\nsettings. Red highlighted values\nare required!"))
                    } +
                    cmp(" | ") +
                    literalText("CANCEL") {
                        color = cError
                        bold = true
                        strikethrough = false
                        clickEvent = ClickEvent.runCommand("$cmd cancel")
                        hoverEvent = HoverEvent.showText(cmp("Cancel the current marker builder.\nThis will delete all your values!"))
                    } + cmp(" ]", cHighlight) + cmp("                 ", cHighlight, strikethrough = true)
        )
    }

    private fun sendAppliedSuccess(sender: CommandSourceStack, message: String, isSet: Boolean = false) {
        sender.bukkitSender.sendMessage(prefix + cmp("Marker${if (isSet) "-Set" else ""} $message applied!", cSuccess))
        sendStatusInfo(sender, isSet)
        sender.player?.playSound(SoundEvents.NOTE_BLOCK_BIT, 1f, 1.3f)
    }

    private fun validateID(id: String): Boolean {
        return id.matches(Regex("[A-Za-z0-9]*")) && !id.contains(' ')
    }

    private fun sendBuildError(sender: CommandSender, cmd: String) {
        sender.sendMessage(prefix + cmp("An unexpected error occurred! Please validate your arguments with ", cError) +
                literalText {
                    component(cmp(cmd, cError, underlined = true))
                    clickEvent = ClickEvent.runCommand(cmd)
                    hoverEvent = HoverEvent.showText(cmp(cmd))
                } +
                cmp(" or report it to the BlueMap Discord (#3rd-party-support)"))
    }

    private fun sendRequiredError(sender: CommandSender, cmd: String) {
        sender.sendMessage(prefix + cmp("A required option is not set! Type ", cError) + literalText {
            component(cmp(cmd, cError, underlined = true))
            clickEvent = ClickEvent.runCommand(cmd)
            hoverEvent = HoverEvent.showText(cmp(cmd))
        } + cmp(" to see more information", cError))
    }

    /*
     * Extensions to prevent duplication
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
                            val color = Color(colorR, colorG, colorB, opacity)
                            val builder = getBuilder(sender) ?: return@runs
                            builder.setArg(arg, ArgumentValue(color))
                            sendAppliedSuccess(sender, "color ${color.stringify()}")
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
                    val label = getArgument<String>("label")
                    val builder = getBuilder(sender, isSet) ?: return@runs
                    builder.setArg(MarkerArg.LABEL, ArgumentValue(label))
                    sendAppliedSuccess(sender, "label '$label'", isSet)
                }
            }
        }
    }

    private fun ArgumentBuilder<CommandSourceStack, *>.idLogic(isSet: Boolean): LiteralArgumentBuilder<CommandSourceStack> {
        return literal("id") {
            argument<String>("id", StringArgumentType.word()) {
                runs {
                    val id = getArgument<String>("id")
                    val builder = getBuilder(sender, isSet) ?: return@runs
                    builder.setArg(MarkerArg.ID, ArgumentValue(id))
                    sendAppliedSuccess(sender, "ID $id", isSet)
                }
            }
        }
    }
}