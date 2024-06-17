package de.miraculixx.bmm.commands

import com.flowpowered.math.vector.Vector3d
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.map.data.Box
import de.miraculixx.bmm.map.data.MarkerTemplateEntry
import de.miraculixx.bmm.map.data.TemplateSet
import de.miraculixx.bmm.map.data.TemplateSetLoader
import de.miraculixx.bmm.utils.data.manageTemplates
import de.miraculixx.bmm.utils.data.templateCommandPrefix
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.locale
import de.miraculixx.mcommons.text.*
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.entity.world
import net.silkmc.silk.core.text.literalText

class TemplateCommand : TemplateCommandInterface, TemplateSetLoader {
    private val templateCommands: MutableMap<String, TemplateCommandImplementation> = mutableMapOf()

    override fun loadTemplate(templateSet: TemplateSet) {
        templateCommands[templateSet.name] = TemplateCommandImplementation(templateSet)
    }

    @Suppress("unused")
    private val templateCommand = command(templateCommandPrefix) {
        requires {
            Permissions.require(manageTemplates, 3).test(it)
        }

        literal("help") {
            runsAsync {
                source.sendMessage(prefix + locale.msg("command.template.help"))
            }
        }

        literal("create") {
            argument<String>("name") { setID ->
                runsAsync {
                    val newSet = source.createNewSet(setID(), false) ?: return@runsAsync
                    templateCommands[setID()] = TemplateCommandImplementation(newSet)
                }
                argument<Boolean>("needsPermission", BoolArgumentType.bool()) { needsPermission ->
                    runsAsync {
                        val newSet = source.createNewSet(setID(), needsPermission()) ?: return@runsAsync
                        templateCommands[setID()] = TemplateCommandImplementation(newSet)
                    }
                }
            }
        }

        literal("delete") {
            argument<String>("name", StringArgumentType.string()) { id ->
                runsAsync { source.deleteSet(id(), false) }
                suggestList { templateCommands.keys }
                argument<Boolean>("confirm", BoolArgumentType.bool()) { confirm ->
                    runsAsync {
                        if (source.deleteSet(id(), confirm())) templateCommands.remove(id())?.unregister()
                    }
                }
            }
        }
    }

    private class TemplateCommandImplementation(
        private val templateSet: TemplateSet
    ) : TemplateCommandInterface {
        private val managePermission = "bmarker.template.${templateSet.name}-manage"

        @Suppress("unused")
        private val command = command(templateSet.name) {
            if (templateSet.needPermission) {
                requires {
                    Permissions.require("bmarker.template.${templateSet.name}", 3).test(it)
                }
            }

            literal("edit") {
                requires {
                    Permissions.require(managePermission, 3).test(it)
                }
                literal("set") {
                    literal("label") {
                        argument<String>("label", StringArgumentType.greedyString()) { label ->
                            runsAsync {
                                source.setSetArg(templateSet, MarkerArg.LABEL, Box.BoxString(label()))
                            }
                        }
                    }
                    applySetArgumentBool("toggleable", MarkerArg.TOGGLEABLE)
                    applySetArgumentBool("hidden", MarkerArg.DEFAULT_HIDDEN)
                }
                literal("markers") {
                    literal("help") {
                        runsAsync { source.sendMessage(locale.msg("command.template.help-marker")) }
                    }
                    literal("add-template") {
                        argument<String>("type", StringArgumentType.word()) { type ->
                            suggestList { listOf("poi", "line", "shape", "extrude", "ellipse") }
                            runsAsync { source.addMarkerTemplate(source.textName, templateSet, type()) }
                        }
                    }
                    literal("remove-template") {
                        argument<String>("id", StringArgumentType.string()) { id ->
                            suggestList { templateSet.templateMarker.keys }
                            runsAsync { source.removeMarkerTemplate(templateSet, id()) }
                        }
                    }
                    literal("edit-template") {
                        argument<String>("id", StringArgumentType.string()) { id ->
                            suggestList { templateSet.templateMarker.keys }
                            runsAsync { source.editMarkerTemplate(source.textName, id(), templateSet) }
                        }
                    }
                }
                literal("maxMarkersPerPlayer") {
                    argument<Int>("amount", IntegerArgumentType.integer(-1)) { amount ->
                        runsAsync {
                            templateSet.maxMarkerPerPlayer = amount()
                            source.sendMessage(prefix + locale.msg("command.template.setArg", listOf(amount().toString())))
                        }
                    }
                }
                literal("maps") {
                    literal("add") {
                        argument<String>("map", StringArgumentType.string()) { map ->
                            runsAsync { source.addMap(templateSet, map()) }
                        }
                    }
                    literal("remove") {
                        argument<String>("map", StringArgumentType.string()) { map ->
                            runsAsync { source.removeMap(templateSet, map()) }
                        }
                    }
                }
            }

            literal("mark") {
                argument<String>("template") { template ->
                    suggestList { templateSet.templateMarker.keys }
                    argument<String>("name", StringArgumentType.greedyString()) { name ->
                        requires { it.isPlayer }
                        runsAsync {
                            val player = source.player ?: return@runsAsync
                            val position = player.position().let { Vector3d(it.x, it.y, it.z) }
                            val entry = MarkerTemplateEntry(template(), player.scoreboardName, name().replace(' ', '_'), position)
                            val bypass = Permissions.require(managePermission, 3).test(source)
                            player.placeMarker(entry, templateSet, bypass, player.world)
                        }
                    }
                }
            }

            literal("unmark") {
                argument<String>("name") { name ->
                    suggestListWithTooltipsSuspending { info ->
                        buildList {
                            templateSet.playerMarkers.filter { it.value.playerName == info.source.textName }.forEach { (key, data) ->
                                add(key to literalText("Template: ").append(literalText(data.templateName) { color = 0x6e94ff }))
                            }
                        }
                    }
                    runsAsync {
                        val bypass = Permissions.require(managePermission, 3).test(source)
                        source.unplaceMarker(templateSet, name(), bypass, source.textName)
                    }
                }
            }
        }

        fun unregister() {
            // TODO unregister command
            templateSet.remove()
        }

        private fun LiteralCommandBuilder<CommandSourceStack>.applySetArgumentBool(name: String, arg: MarkerArg) {
            literal(name) {
                argument<Boolean>(name, BoolArgumentType.bool()) { name ->
                    runsAsync { source.setSetArg(templateSet, arg, Box.BoxBoolean(name())) }
                }
            }
        }
    }
}