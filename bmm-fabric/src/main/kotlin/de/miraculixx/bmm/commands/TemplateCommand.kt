@file:OptIn(ExperimentalSilkApi::class, InternalSilkApi::class)

package de.miraculixx.bmm.commands

import com.flowpowered.math.vector.Vector3d
import com.mojang.brigadier.CommandDispatcher
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
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.silkmc.silk.commands.LiteralCommandBuilder
import net.silkmc.silk.commands.command
import net.silkmc.silk.commands.event.Command
import net.silkmc.silk.core.Silk
import net.silkmc.silk.core.annotations.ExperimentalSilkApi
import net.silkmc.silk.core.annotations.InternalSilkApi
import net.silkmc.silk.core.entity.world
import net.silkmc.silk.core.event.Events
import net.silkmc.silk.core.server.players
import net.silkmc.silk.core.text.literalText

/**
 * Every template set gets its own root command. Silk can only register commands while the
 * `Commands` instance is being built, so the dispatcher and its build context are captured from that
 * event and reused to attach commands of templates that are created later at runtime.
 * Brigadier cannot remove nodes again
 */
class TemplateCommand : TemplateCommandInterface, TemplateSetLoader {
    private var dispatcher: CommandDispatcher<CommandSourceStack>? = null
    private var buildContext: CommandBuildContext? = null
    private val attached = mutableSetOf<String>()

    init {
        Events.Command.register.monitor { event ->
            // A fresh dispatcher is built on every server start and on every /reload
            dispatcher = event.dispatcher
            buildContext = event.context
            attached.clear()
            MarkerManager.templateSets.keys.toList().forEach { attach(it) }
        }
    }

    override fun loadTemplate(templateSet: TemplateSet) {
        attach(templateSet.name)
    }

    private fun attach(name: String) {
        val dispatcher = dispatcher ?: return // Registered as soon as the dispatcher exists
        val context = buildContext ?: return
        if (!attached.add(name)) return
        if (dispatcher.root.getChild(name) != null) return // Do not shadow another command
        buildTemplateCommand(name).commandBuilder.toBrigadier(context).forEach { dispatcher.root.addChild(it) }
        resendCommands()
    }

    /**
     * Push the changed command tree to all players, otherwise the client keeps suggesting the old one.
     */
    private fun resendCommands() {
        val server = Silk.server ?: return
        if (server.commands.dispatcher !== dispatcher) return // Still being built, clients get it anyway
        server.players.forEach { server.commands.sendCommands(it) }
    }

    private fun templateOf(name: String) = MarkerManager.templateSets[name]

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
                    source.createNewSet(setID(), false) ?: return@runsAsync
                    attach(setID())
                }
                argument<Boolean>("needsPermission", BoolArgumentType.bool()) { needsPermission ->
                    runsAsync {
                        source.createNewSet(setID(), needsPermission()) ?: return@runsAsync
                        attach(setID())
                    }
                }
            }
        }

        literal("delete") {
            argument<String>("name", StringArgumentType.string()) { id ->
                runsAsync { source.deleteSet(id(), false) }
                suggestList { MarkerManager.templateSets.keys }
                argument<Boolean>("confirm", BoolArgumentType.bool()) { confirm ->
                    runsAsync {
                        if (source.deleteSet(id(), confirm())) resendCommands()
                    }
                }
            }
        }
    }

    private fun buildTemplateCommand(name: String) = command(name, register = false) {
        val managePermission = "bmarker.template.$name-manage"

        requires { source ->
            val templateSet = templateOf(name) ?: return@requires false // Template was deleted
            !templateSet.needPermission || Permissions.require("bmarker.template.$name", 3).test(source)
        }

        literal("edit") {
            requires {
                Permissions.require(managePermission, 3).test(it)
            }
            literal("set") {
                literal("label") {
                    argument<String>("label", StringArgumentType.greedyString()) { label ->
                        runsAsync {
                            val templateSet = templateOf(name) ?: return@runsAsync
                            source.setSetArg(templateSet, MarkerArg.LABEL, Box.BoxString(label()))
                        }
                    }
                }
                applySetArgumentBool(name, "toggleable", MarkerArg.TOGGLEABLE)
                applySetArgumentBool(name, "hidden", MarkerArg.DEFAULT_HIDDEN)
            }
            literal("markers") {
                literal("help") {
                    runsAsync { source.sendMessage(locale.msg("command.template.help-marker")) }
                }
                literal("add-template") {
                    argument<String>("type", StringArgumentType.word()) { type ->
                        suggestList { listOf("poi", "line", "shape", "extrude", "ellipse") }
                        runsAsync {
                            val templateSet = templateOf(name) ?: return@runsAsync
                            source.addMarkerTemplate(source.textName, templateSet, type())
                        }
                    }
                }
                literal("remove-template") {
                    argument<String>("id", StringArgumentType.string()) { id ->
                        suggestList { templateOf(name)?.templateMarker?.keys.orEmpty() }
                        runsAsync {
                            val templateSet = templateOf(name) ?: return@runsAsync
                            source.removeMarkerTemplate(templateSet, id())
                        }
                    }
                }
                literal("edit-template") {
                    argument<String>("id", StringArgumentType.string()) { id ->
                        suggestList { templateOf(name)?.templateMarker?.keys.orEmpty() }
                        runsAsync {
                            val templateSet = templateOf(name) ?: return@runsAsync
                            source.editMarkerTemplate(source.textName, id(), templateSet)
                        }
                    }
                }
            }
            literal("maxMarkersPerPlayer") {
                argument<Int>("amount", IntegerArgumentType.integer(-1)) { amount ->
                    runsAsync {
                        val templateSet = templateOf(name) ?: return@runsAsync
                        templateSet.maxMarkerPerPlayer = amount()
                        MarkerManager.saveTemplate(name)
                        source.sendMessage(prefix + locale.msg("command.template.setArg", listOf(amount().toString())))
                    }
                }
            }
            literal("maps") {
                literal("add") {
                    argument<String>("map", StringArgumentType.string()) { map ->
                        runsAsync {
                            val templateSet = templateOf(name) ?: return@runsAsync
                            source.addMap(templateSet, map())
                        }
                    }
                }
                literal("remove") {
                    argument<String>("map", StringArgumentType.string()) { map ->
                        runsAsync {
                            val templateSet = templateOf(name) ?: return@runsAsync
                            source.removeMap(templateSet, map())
                        }
                    }
                }
            }
        }

        literal("mark") {
            argument<String>("template") { template ->
                suggestList { templateOf(name)?.templateMarker?.keys.orEmpty() }
                argument<String>("name", StringArgumentType.greedyString()) { markerName ->
                    requires { it.isPlayer }
                    runsAsync {
                        val templateSet = templateOf(name) ?: return@runsAsync
                        val player = source.player ?: return@runsAsync
                        val position = player.position().let { Vector3d(it.x, it.y, it.z) }
                        val entry = MarkerTemplateEntry(template(), player.scoreboardName, markerName().replace(' ', '_'), position)
                        val bypass = Permissions.require(managePermission, 3).test(source)
                        player.placeMarker(entry, templateSet, bypass, player.world)
                    }
                }
            }
        }

        literal("unmark") {
            argument<String>("name") { markerName ->
                suggestListWithTooltipsSuspending { info ->
                    buildList {
                        templateOf(name)?.playerMarkers?.filter { it.value.playerName == info.source.textName }?.forEach { (key, data) ->
                            add(key to literalText("Template: ").append(literalText("${data.templateName} (${data.position.x.toInt()}, ${data.position.y.toInt()}, ${data.position.z.toInt()})") { color = 0x6e94ff }))
                        }
                    }
                }
                runsAsync {
                    val templateSet = templateOf(name) ?: return@runsAsync
                    val bypass = Permissions.require(managePermission, 3).test(source)
                    source.unplaceMarker(templateSet, markerName(), bypass, source.textName)
                }
            }
        }
    }

    private fun LiteralCommandBuilder<CommandSourceStack>.applySetArgumentBool(templateName: String, name: String, arg: MarkerArg) {
        literal(name) {
            argument<Boolean>(name, BoolArgumentType.bool()) { value ->
                runsAsync {
                    val templateSet = templateOf(templateName) ?: return@runsAsync
                    source.setSetArg(templateSet, arg, Box.BoxBoolean(value()))
                }
            }
        }
    }
}
