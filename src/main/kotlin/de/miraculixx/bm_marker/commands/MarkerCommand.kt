package de.miraculixx.bm_marker.commands

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector2i
import com.flowpowered.math.vector.Vector3d
import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import de.bluecolored.bluemap.api.math.Color
import de.miraculixx.bm_marker.map.ArgumentValue
import de.miraculixx.bm_marker.map.MarkerBuilder
import de.miraculixx.bm_marker.map.MarkerManager
import de.miraculixx.bm_marker.utils.enumOf
import de.miraculixx.bm_marker.utils.enums.MarkerArg
import de.miraculixx.bm_marker.utils.enums.MarkerType
import de.miraculixx.bm_marker.utils.message.*
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.commands.*
import net.axay.kspigot.extensions.broadcast
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

class MarkerCommand {
    private val builder: MutableMap<String, MarkerBuilder> = mutableMapOf()
    private val setupCommandPrefix = "bmarker-setup"

    val mainCommand = command("bmarker") {
        literal("create") {
            argument<String>("type", StringArgumentType.word()) {
                suggestList { listOf("poi", "html", "line", "shape", "extrude") }
                runs {
                    if (builder.contains(sender.textName)) {
                        sender.bukkitSender.sendMessage(prefix + cmp("You already started a marker setup! ", cError) + literalText {
                            component(cmp("Cancel", cError, underlined = true))
                            clickEvent = ClickEvent.runCommand("/bmarker cancel")
                            hoverEvent = HoverEvent.showText(cmp("/bmarker cancel"))
                        } + cmp(" or ", cError) + literalText {
                            component(cmp("build", cError, underlined = true))
                            clickEvent = ClickEvent.runCommand("/bmarker build")
                            hoverEvent = HoverEvent.showText(cmp("/bmarker build"))
                        } + cmp(" it before creating a new one", cError))
                    } else {
                        val type = getArgument<String>("type")

                        val markerType = enumOf<MarkerType>(type.uppercase())
                        if (markerType == null) {
                            sender.bukkitSender.sendMessage(prefix + cmp("This is not a valid marker!", cError))
                            return@runs
                        } else if (markerType == MarkerType.HTML) {
                            sender.bukkitSender.sendMessage(
                                prefix + cmp("Due Minecraft's chat limitations this marker cannot be set ingame! Stick to the BlueMap wiki to set it up - ", cError)
                                        + cmp("[wiki]").clickEvent(ClickEvent.openUrl("https://bluemap.bluecolored.de/wiki/customization/Markers.html#html-markers"))
                            )
                            return@runs
                        }
                        builder[sender.textName] = MarkerBuilder(markerType)
                        broadcast(sender.textName)
                        sender.bukkitSender.sendMessage(prefix + cmp("Marker setup started! Modify values using ") + literalText {
                            component(cmp("/bmarker-setup", cMark, underlined = true))
                            clickEvent = ClickEvent.suggestCommand("/bmarker-setup ")
                            hoverEvent = HoverEvent.showText(cmp("Use /bmarker-setup <arg> <value>"))
                        } + cmp(" and finish your setup with ") + literalText {
                            component(cmp("/bmarker build", cMark, underlined = true))
                            clickEvent = ClickEvent.runCommand("/bmarker build")
                            hoverEvent = HoverEvent.showText(cmp("/bmarker build"))
                        })
                        sendStatusInfo(sender)
                    }
                }
            }
        }
        literal("build") {
            runs {
                if (!builder.contains(sender.textName)) noMarkerBuilder(sender)
                else {
                    val build = builder[sender.textName]
                    val worldName = build?.args?.get(MarkerArg.WORLD)?.getString()
                    val markerID = build?.args?.get(MarkerArg.ID)?.getString()
                    val bukkitSender = sender.bukkitSender
                    if (worldName == null || markerID == null) {
                        bukkitSender.sendMessage(prefix + cmp("Please provide a marker ID and a target world!", cError))
                        return@runs
                    }

                    val marker = try {
                        build.buildMarker()
                    } catch (e: Exception) {
                        bukkitSender.sendMessage(prefix + cmp("An unexpected error occurred! Please validate your arguments with ", cError) + literalText {
                            val cmd = "/$setupCommandPrefix"
                            component(cmp(cmd, cError, underlined = true))
                            clickEvent = ClickEvent.runCommand(cmd)
                            hoverEvent = HoverEvent.showText(cmp(cmd))
                        } + cmp(" or report it"))
                        e.printStackTrace()
                        return@runs
                    }
                    if (marker == null) {
                        bukkitSender.sendMessage(prefix + cmp("A required option is not set! Type ", cError) + literalText {
                            component(cmp("/bmarker-setup", cError, underlined = true))
                            clickEvent = ClickEvent.runCommand("/bmarker-setup")
                            hoverEvent = HoverEvent.showText(cmp("/bmarker-setup"))
                        } + cmp(" to see more information", cError))
                        return@runs
                    }
                    MarkerManager.addMarker(worldName, marker, markerID)
                    builder.remove(sender.textName)

                    bukkitSender.sendMessage(prefix + cmp("Marker created! It should appear on your BlueMap in a few seconds"))
                }
            }
        }
        literal("delete") {
            argument<ResourceLocation>("world", DimensionArgument()) {
                argument<String>("marker-id", StringArgumentType.word()) {
                    suggestList { ctx -> MarkerManager.getAllMarkers(worldName = ctx.getArgument<ResourceLocation>("world").path).keys }
                    runs {
                        val world = getArgument<ResourceLocation>("world").path
                        val markerID = getArgument<String>("marker-id")
                        MarkerManager.removeMarker(world, markerID)
                        sender.bukkitSender.sendMessage(prefix + cmp("Successfully deleted ") + cmp(markerID, cMark) + cmp(" marker! It should disappear from your BlueMap in a few seconds"))
                    }
                }
            }
        }
        literal("cancel") {
            runs {
                if (builder.remove(sender.textName) == null) noMarkerBuilder(sender)
                else sender.bukkitSender.sendMessage(prefix + cmp("Canceled current marker setup!"))
            }
        }

    }

    val enterCommand = command(setupCommandPrefix) {
        runs {
            sendStatusInfo(sender)
        }

        // Single String / URL
        literal("icon") {
            argument<String>("icon", StringArgumentType.word()) {
                runs {
                    val icon = getArgument<String>("icon")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.args[MarkerArg.ICON] = ArgumentValue(icon)
                    sendAppliedSuccess(sender, "icon URL $icon")
                }
            }
        }
        literal("link") {
            argument<String>("link", StringArgumentType.greedyString()) {
                runs {
                    val link = getArgument<String>("link")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.args[MarkerArg.LINK] = ArgumentValue(link)
                    sendAppliedSuccess(sender, "link URL $link")
                }
            }
        }
        literal("id") {
            argument<String>("id", StringArgumentType.word()) {
                runs {
                    val id = getArgument<String>("id")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.args[MarkerArg.ID] = ArgumentValue(id)
                    sendAppliedSuccess(sender, "ID $id")
                }
            }
        }

        // Multi Strings
        literal("label") {
            argument<String>("label", StringArgumentType.greedyString()) {
                runs {
                    val label = getArgument<String>("label")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.args[MarkerArg.LABEL] = ArgumentValue(label)
                    sendAppliedSuccess(sender, "label $label")
                }
            }
        }
        literal("detail") {
            argument<String>("detail", StringArgumentType.greedyString()) {
                runs {
                    val detail = getArgument<String>("detail")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.args[MarkerArg.DETAIL] = ArgumentValue(detail)
                    sendAppliedSuccess(sender, "detail $detail")
                }
            }
        }

        // Dimensions / Worlds
        literal("world") {
            argument<ResourceLocation>("world", DimensionArgument()) {
                runs {
                    val world = getArgument<ResourceLocation>("world").path
                    val builder = getBuilder(sender) ?: return@runs
                    builder.args[MarkerArg.WORLD] = ArgumentValue(world)
                    sendAppliedSuccess(sender, "world $world")
                }
            }
        }

        // Locations
        literal("position") {
            argument<Coordinates>("position", Vec3Argument(true)) {
                runs {
                    val position = getArgument<Coordinates>("position").getPosition(sender)
                    val vec3d = Vector3d(position.x, position.y, position.z)
                    val builder = getBuilder(sender) ?: return@runs
                    builder.args[MarkerArg.POSITION] = ArgumentValue(vec3d)
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
                    builder.args[MarkerArg.ANCHOR] = ArgumentValue(vec2d)
                    sendAppliedSuccess(sender, "anchor $vec2d")
                }
            }
        }
        literal("add_direction") {
            argument<Coordinates>("add-direction", Vec3Argument(true)) {
                runs {
                    val newDirection = getArgument<Coordinates>("add-direction").getPosition(sender)
                    val vec3d = Vector3d(newDirection.x, newDirection.y, newDirection.z)
                    val builder = getBuilder(sender) ?: return@runs
                    builder.vector3dList.add(vec3d)
                    sendAppliedSuccess(sender, "new direction $vec3d")
                }
            }
        }
        literal("add_edge") {
            argument<Coordinates>("add-edge", Vec2Argument(true)) {
                runs {
                    val edge = getArgument<Coordinates>("add-edge").getPosition(sender)
                    val vec2d = Vector2d(edge.x, edge.z)
                    val builder = getBuilder(sender) ?: return@runs
                    builder.vector2dList.add(vec2d)
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
                    builder.args[MarkerArg.MAX_DISTANCE] = ArgumentValue(maxDistance)
                    sendAppliedSuccess(sender, "maximal distance $maxDistance")
                }
            }
        }
        literal("min_distance") {
            argument<Double>("min-distance", DoubleArgumentType.doubleArg(0.0)) {
                runs {
                    val minDistance = getArgument<Double>("min-distance")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.args[MarkerArg.MIN_DISTANCE] = ArgumentValue(minDistance)
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
                    builder.args[MarkerArg.LINE_WIDTH] = ArgumentValue(lineWidth)
                    sendAppliedSuccess(sender, "line width $lineWidth")
                }
            }
        }

        // Floats
        literal("height") {
            argument<Float>("height", FloatArgumentType.floatArg()) {
                runs {
                    val height = getArgument<Int>("height")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.args[MarkerArg.HEIGHT] = ArgumentValue(height)
                    sendAppliedSuccess(sender, "height $height")
                }
            }
        }
        literal("max-height") {
            argument<Float>("max-height", FloatArgumentType.floatArg()) {
                runs {
                    val maxHeight = getArgument<Int>("max-height")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.args[MarkerArg.MAX_HEIGHT] = ArgumentValue(maxHeight)
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
                    builder.args[MarkerArg.NEW_TAB] = ArgumentValue(newTab)
                    sendAppliedSuccess(sender, "open new tab on click $newTab")
                }
            }
        }
        literal("depth_test") {
            argument<Boolean>("depth-test", BoolArgumentType.bool()) {
                runs {
                    val depthTest = getArgument<Boolean>("depth-test")
                    val builder = getBuilder(sender) ?: return@runs
                    builder.args[MarkerArg.DEPTH_TEST] = ArgumentValue(depthTest)
                    sendAppliedSuccess(sender, "depth test $depthTest")
                }
            }
        }
    }

    private fun noMarkerBuilder(sender: CommandSourceStack) {
        sender.bukkitSender.sendMessage(
            prefix + cmp("You have no current marker setups. Start one with ", cError) + literalText {
                component(cmp("/bmarker create", cError, underlined = true))
                clickEvent = ClickEvent.suggestCommand("/bmarker create ")
                hoverEvent = HoverEvent.showText(cmp("Start a marker setup (click)"))
            }
        )
    }

    private fun getBuilder(sender: CommandSourceStack): MarkerBuilder? {
        return builder.getOrElse(sender.textName) {
            noMarkerBuilder(sender)
            return null
        }
    }

    private fun sendStatusInfo(sender: CommandSourceStack) {
        val builder = getBuilder(sender) ?: return
        val bukkitSender = sender.bukkitSender
        val type = builder.type
        val appliedArgs = builder.args
        val nothingSet = cmp("Not Set", italic = true)
        val dash = cmp("- ")
        val midDash = cmp(" ≫ ", NamedTextColor.DARK_GRAY)
        bukkitSender.sendMessage(cmp(" \n") + prefix + cmp("Your current setup state (${type.name})"))
        type.args.forEach { arg ->
            // List values displayed in a different way than single values
            if (arg == MarkerArg.ADD_DIRECTION || arg == MarkerArg.ADD_EDGE) {
                bukkitSender.sendMessage(dash + literalText {
                    val list = when (arg) {
                        MarkerArg.ADD_DIRECTION -> builder.vector3dList
                        MarkerArg.ADD_EDGE -> builder.vector2dList
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
                    clickEvent = ClickEvent.suggestCommand("/$setupCommandPrefix ${arg.name.lowercase()} ")
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
                clickEvent = ClickEvent.suggestCommand("/$setupCommandPrefix ${arg.name.lowercase()} ")
            })
        }
        bukkitSender.sendMessage(
            cmp("                 ", cHighlight, strikethrough = true) + cmp("[ ", cHighlight, strikethrough = false) +
                    literalText("BUILD") {
                        color = cSuccess
                        bold = true
                        strikethrough = false
                        clickEvent = ClickEvent.runCommand("/bmarker build")
                        hoverEvent = HoverEvent.showText(cmp("Build a new marker with applied\nsettings. Red highlighted values\nare required!"))
                    } +
                    cmp(" | ") +
                    literalText("CANCEL") {
                        color = cError
                        bold = true
                        strikethrough = false
                        clickEvent = ClickEvent.runCommand("/bmarker cancel")
                        hoverEvent = HoverEvent.showText(cmp("Cancel the current marker builder.\nThis will delete all your values!"))
                    } + cmp(" ]", cHighlight) + cmp("                 ", cHighlight, strikethrough = true)
        )
    }

    private fun sendAppliedSuccess(sender: CommandSourceStack, message: String) {
        sender.bukkitSender.sendMessage(prefix + cmp("Marker $message applied!", cSuccess))
        sendStatusInfo(sender)
        sender.player?.playSound(SoundEvents.NOTE_BLOCK_BIT, 1f, 1.3f)
    }

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
                            builder.args[arg] = ArgumentValue(color)
                            sendAppliedSuccess(sender, "color ${color.stringify()}")
                        }
                    }
                }
            }
        }
    }
}