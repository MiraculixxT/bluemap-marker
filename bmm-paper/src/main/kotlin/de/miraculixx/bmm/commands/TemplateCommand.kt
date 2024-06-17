package de.miraculixx.bmm.commands

import com.flowpowered.math.vector.Vector3d
import de.miraculixx.bmm.anyExecutorAsync
import de.miraculixx.bmm.map.data.Box
import de.miraculixx.bmm.map.data.MarkerTemplateEntry
import de.miraculixx.bmm.map.data.TemplateSet
import de.miraculixx.bmm.map.data.TemplateSetLoader
import de.miraculixx.bmm.playerExecutorAsync
import de.miraculixx.bmm.utils.data.manageTemplates
import de.miraculixx.bmm.utils.data.templateCommandPrefix
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.locale
import de.miraculixx.mcommons.text.*
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.*
import io.papermc.paper.adventure.AdventureComponent
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrDefault

class TemplateCommand : TemplateCommandInterface, TemplateSetLoader {
    private val templateCommands: MutableMap<String, TemplateCommandImplementation> = mutableMapOf()

    override fun loadTemplate(templateSet: TemplateSet) {
        templateCommands[templateSet.name] = TemplateCommandImplementation(templateSet)
    }

    @Suppress("unused")
    private val templateCommand = commandTree(templateCommandPrefix) {
        withPermission(manageTemplates)

        literalArgument("help") {
            anyExecutorAsync { sender, _ ->
                sender.sendMessage(prefix + locale.msg("command.template.help"))
            }
        }

        literalArgument("create") {
            stringArgument("name") {
                booleanArgument("needsPermission", true) {
                    anyExecutorAsync { sender, args ->
                        val setID = args[0] as String
                        val newSet = sender.createNewSet(setID, args.getOptional(1).getOrDefault(false) as Boolean) ?: return@anyExecutorAsync
                        templateCommands[setID] = TemplateCommandImplementation(newSet)
                    }
                }
            }
        }

        literalArgument("delete") {
            stringArgument("name") {
                replaceSuggestions(ArgumentSuggestions.stringCollection { templateCommands.keys })
                booleanArgument("confirm", true) {
                    anyExecutorAsync { sender, args ->
                        val confirm = args.getOptional(0).getOrDefault(false) as Boolean
                        val id = args[0] as String
                        if (sender.deleteSet(id, confirm)) templateCommands.remove(id)?.unregister()
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
        private val command = commandTree(templateSet.name) {
            if (templateSet.needPermission) withPermission("bmarker.template.${templateSet.name}")
            literalArgument("edit") {
                withPermission(managePermission)
                literalArgument("set") {
                    literalArgument("label") {
                        greedyStringArgument("label") {
                            anyExecutorAsync { sender, args ->
                                sender.setSetArg(templateSet, MarkerArg.LABEL, Box.BoxString(args[0] as String))
                            }
                        }
                    }
                    applySetArgumentBool("toggleable", MarkerArg.TOGGLEABLE)
                    applySetArgumentBool("hidden", MarkerArg.DEFAULT_HIDDEN)
                }
                literalArgument("markers") {
                    literalArgument("help") {
                        anyExecutorAsync { sender, _ -> sender.sendMessage(locale.msg("command.template.help-marker")) }
                    }
                    literalArgument("add-template") {
                        textArgument("type") {
                            replaceSuggestions(ArgumentSuggestions.strings(listOf("poi", "line", "shape", "extrude", "ellipse")))
                            anyExecutorAsync { sender, args -> sender.addMarkerTemplate(sender.name, templateSet, args[0] as String) }
                        }
                    }
                    literalArgument("remove-template") {
                        textArgument("id") {
                            replaceSuggestions(ArgumentSuggestions.stringCollection { templateSet.templateMarker.keys })
                            anyExecutorAsync { sender, args -> sender.removeMarkerTemplate(templateSet, args[0] as String) }
                        }
                    }
                    literalArgument("edit-template") {
                        textArgument("id") {
                            replaceSuggestions(ArgumentSuggestions.stringCollection { templateSet.templateMarker.keys })
                            anyExecutorAsync { sender, args -> sender.editMarkerTemplate(sender.name, args[0] as String, templateSet) }
                        }
                    }
                }
                literalArgument("maxMarkersPerPlayer") {
                    integerArgument("amount") {
                        anyExecutorAsync { sender, args ->
                            templateSet.maxMarkerPerPlayer = args[0] as Int
                            sender.sendMessage(prefix + locale.msg("command.template.setArg", listOf(args[0].toString())))
                        }
                    }
                }
                literalArgument("maps") {
                    literalArgument("add") {
                        textArgument("map") {
                            anyExecutorAsync { sender, args -> sender.addMap(templateSet, args[0] as String) }
                        }
                    }
                    literalArgument("remove") {
                        textArgument("map") {
                            anyExecutorAsync { sender, args -> sender.removeMap(templateSet, args[0] as String) }
                        }
                    }
                }
            }

            literalArgument("mark") {
                textArgument("template") {
                    replaceSuggestions(ArgumentSuggestions.stringCollection { templateSet.templateMarker.keys })
                    greedyStringArgument("name") {
                        playerExecutorAsync { player, args ->
                            val templateName = args[0] as String
                            val markerName = args[1] as String
                            val position = player.location.let { Vector3d(it.x, it.y, it.z) }
                            val entry = MarkerTemplateEntry(templateName, player.name, markerName.replace(' ', '_'), position)
                            player.placeMarker(entry, templateSet, player.hasPermission(managePermission), player.world)
                        }
                    }
                }
            }

            literalArgument("unmark") {
                textArgument("name") {
                    replaceSuggestions { info, builder ->
                        CompletableFuture.supplyAsync {
                            templateSet.playerMarkers.filter { it.value.playerName == info.sender.name }.forEach { (key, data) ->
                                builder.suggest(key, AdventureComponent(cmp("Template: ") + cmp(data.templateName, cMark)))
                            }
                            builder.build()
                        }
                    }
                    playerExecutorAsync { player, args ->
                        player.unplaceMarker(templateSet, args[0] as String, player.hasPermission(managePermission), player.name)
                    }
                }
            }
        }

        fun unregister() {
            CommandAPI.unregister(templateSet.name)
            templateSet.remove()
        }

        private fun Argument<*>.applySetArgumentBool(name: String, arg: MarkerArg) {
            literalArgument(name) {
                booleanArgument(name) {
                    anyExecutorAsync { sender, args -> sender.setSetArg(templateSet, arg, Box.BoxBoolean(args[0] as Boolean)) }
                }
            }
        }
    }
}