package de.miraculixx.bmm

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector2i
import com.flowpowered.math.vector.Vector3d
import de.bluecolored.bluemap.api.math.Color
import de.miraculixx.bmm.map.MarkerBuilder
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.map.MarkerSetBuilder
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.message.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class MarkerCommand : TabExecutor, MarkerCommandInstance {
    override val builder: MutableMap<String, MarkerBuilder> = mutableMapOf()
    override val builderSet: MutableMap<String, MarkerSetBuilder> = mutableMapOf()

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>?): MutableList<String> {
        return buildList {
            when (args?.size ?: 0) {
                0, 1 -> addAll(listOf("create", "delete", "edit", "set-create", "set-delete"))
                2 -> when (args?.getOrNull(0) ?: "") {
                    "create" -> addAll(listOf("extrude", "poi", "line", "shape"))
                    "delete", "edit", "set-delete" -> addAll(MarkerManager.getAllMaps())
                }

                3 -> when (args?.getOrNull(0) ?: "") {
                    "delete", "edit", "set-delete" -> addAll(MarkerManager.getAllSetIDs(args?.getOrNull(2) ?: ""))
                }

                4 -> when (args?.getOrNull(0) ?: "") {
                    "delete", "edit" -> addAll(MarkerManager.getAllMarkers(args?.getOrNull(2) ?: "").keys)
                }
            }
        }.filter { it.startsWith(args?.lastOrNull() ?: "", true) }.toMutableList()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (args == null || args.isEmpty()) {
            sender.sendMessage(prefix + cmp("Provide a valid sub command to get started!", cError))
            return false
        }

        when (args.getOrNull(0)?.lowercase()) {
            "create" -> if (sender.hasPermission("bmarker.command.create"))
                create(sender, sender.name, args.getOrNull(1))
            else noPermission("bmarker.command.create", sender)

            "edit" -> if (sender.hasPermission("bmarker.command.edit"))
                edit(sender, sender.name, args.getOrNull(2), args.getOrNull(3))
            else noPermission("bmarker.command.edit", sender)

            "delete" -> if (sender.hasPermission("bmarker.command.delete"))
                delete(sender, sender.name, args.getOrNull(2), args.getOrNull(3))
            else noPermission("bmarker.command.delete", sender)

            "set-create" -> if (sender.hasPermission("bmarker.command.set-create"))
                createSet(sender, sender.name)
            else noPermission("bmarker.command.set-create", sender)

            "set-delete" -> if (sender.hasPermission("bmarker.command.set-delete")) {
                val confirm = args.getOrNull(3)?.toBoolean()
                if (confirm == true) deleteSet(sender, true, args.getOrNull(2), args.getOrNull(1))
            } else noPermission("bmarker.command.set-delete", sender)
        }

        return true
    }

    private fun noPermission(perm: String, sender: CommandSender) {
        sender.sendMessage(prefix + cmp("Missing permissions to perform this command - ", cError) + cmp(perm))
    }

    private class Setup(val builder: MarkerCommand): TabExecutor {
        override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>?): MutableList<String> {
            return buildList {
                when (args?.size ?: 0) {
                    0, 1 -> addAll(listOf("build", "cancel", "icon", "link", "id", "label", "detail", "marker_set", "position", "anchor", "add_direction", "add_edge", "max_distance", "min_distance", "line_width", "height", "line_color", "fill_color", "new_tab", "depth_test"))
                    2 -> when (args?.getOrNull(0)) {
                        "position", "add_direction" -> add("~ ~ ~")
                        "anchor", "add_edge" -> add("~ ~")
                        "line_color", "fill_color" -> add("<red 0-255>")
                        "new_tab", "depth_test" -> addAll(listOf("true","false"))
                        "max_distance", "min_distance", "line_width", "height", "max_height" -> add("<positiv-number>")
                        "id" -> add("<word>")
                        "label", "detail" -> add("<phrase>")
                        "link", "icon" -> add("<url>")
                        "marker_set" -> addAll(MarkerManager.getAllSetIDs())
                    }
                    3 -> when (args?.getOrNull(0)) {
                        "line_color", "fill_color" -> add("<green 0-255>")
                        "label", "detail" -> add("<phrase>")
                    }
                    4 -> when (args?.getOrNull(0)) {
                        "line_color", "fill_color" -> add("<blue 0-255>")
                        "label", "detail" -> add("<phrase>")
                    }
                    5 -> when (args?.getOrNull(0)) {
                        "line_color", "fill_color" -> add("<opacity 0-1>")
                        "label", "detail" -> add("<phrase>")
                    }
                    else -> when (args?.getOrNull(0)) {
                        "label", "detail" -> add("<phrase>")
                    }
                }
            }.toMutableList()
        }

        override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
            val name = sender.name
            val value = args?.getOrNull(1) ?: ""
            when (args?.getOrNull(0)?.lowercase()) {
                "build" -> builder.build(sender, name)
                "cancel" -> builder.cancel(sender, name)
                "icon" -> builder.setMarkerArgument(sender, name, MarkerArg.ICON, value, "icon url $value")
                "link" -> builder.setMarkerArgument(sender, name, MarkerArg.LINK, value, "link url $value")
                "id" -> builder.setMarkerArgument(sender, name, MarkerArg.ID, value, "id $value")
                "label" -> {
                    val v = getMultiString(args)
                    builder.setMarkerArgument(sender, name, MarkerArg.LABEL, v, "label $v")
                }
                "detail" -> {
                    val v = getMultiString(args)
                    builder.setMarkerArgument(sender, name, MarkerArg.DETAIL, v, "detail $v")
                }
                "marker_set" -> builder.setMarkerArgument(sender, name, MarkerArg.MARKER_SET, value, "set $value")
                "position" -> {
                    val position = parse3d(getMultiString(args), sender)
                    builder.setMarkerArgument(sender, name, MarkerArg.POSITION, position, "position $position")
                }
                "anchor" -> {
                    val position = parse2i(getMultiString(args), sender)
                    builder.setMarkerArgument(sender, name, MarkerArg.ANCHOR, position, "anchor $position")
                }
                "add_direction" -> {
                    val position = parse3d(getMultiString(args), sender)
                    builder.addMarkerArgumentList(sender, name, MarkerArg.ADD_POSITION, position, "new direction $position")
                }
                "add_edge" -> {
                    val position = parse2d(getMultiString(args), sender)
                    builder.addMarkerArgumentList(sender, name, MarkerArg.ADD_EDGE, position, "new edge $position")
                }
                "max_distance" -> builder.setMarkerArgument(sender, name, MarkerArg.MAX_DISTANCE, value, "maximal distance $value")
                "min_distance" -> builder.setMarkerArgument(sender, name, MarkerArg.MIN_DISTANCE, value, "minimal distance $value")
                "line_width" -> builder.setMarkerArgument(sender, name, MarkerArg.LINE_WIDTH, value, "line width $value")
                "height" -> builder.setMarkerArgument(sender, name, MarkerArg.HEIGHT, value, "height $value")
                "max_height" -> builder.setMarkerArgument(sender, name, MarkerArg.MAX_HEIGHT, value, "maximal height $value")
                "line_color" -> {
                    val color = parseColor(getMultiString(args))
                    builder.setMarkerArgument(sender, name, MarkerArg.LINE_COLOR, color, "line color ${color.stringify()}")
                }
                "fill_color" -> {
                    val color = parseColor(getMultiString(args))
                    builder.setMarkerArgument(sender, name, MarkerArg.FILL_COLOR, color, "fill color ${color.stringify()}")
                }
                "new_tab" -> builder.setMarkerArgument(sender, name, MarkerArg.NEW_TAB, value, "open new tab $value")
                "depth_test" -> builder.setMarkerArgument(sender, name, MarkerArg.DEPTH_TEST, value, "depth test $value")

                else -> builder.sendStatusInfo(sender, name)
            }
            return true
        }

        private fun parse3d(value: String, sender: CommandSender): Vector3d {
            if (value.contains("~ ~ ~") && sender is Player) {
                val loc = sender.location
                return Vector3d.from(loc.x.round(2), loc.y.round(2), loc.z.round(2))
            }
            val split = value.split(' ', limit = 3)
            return Vector3d.from(
                split.getOrNull(0)?.toDoubleOrNull() ?: .0,
                split.getOrNull(1)?.toDoubleOrNull() ?: .0,
                split.getOrNull(2)?.toDoubleOrNull() ?: .0
            )
        }

        private fun parse2d(value: String, sender: CommandSender): Vector2d {
            if (value.contains("~ ~") && sender is Player) {
                val loc = sender.location
                return Vector2d(loc.x.round(2), loc.z.round(2))
            }
            val split = value.split(' ', limit = 2)
            return Vector2d(
                split.getOrNull(0)?.toDoubleOrNull() ?: .0,
                split.getOrNull(1)?.toDoubleOrNull() ?: .0,
            )
        }

        private fun parse2i(value: String, sender: CommandSender): Vector2i {
            if (value.contains("~ ~") && sender is Player) {
                val loc = sender.location
                return Vector2i(loc.x.round(2), loc.z.round(2))
            }
            val split = value.split(' ', limit = 2)
            return Vector2i(
                split.getOrNull(0)?.toDoubleOrNull() ?: .0,
                split.getOrNull(1)?.toDoubleOrNull() ?: .0,
            )
        }

        private fun parseColor(value: String): Color {
            val split = value.split(' ', limit = 4)
            return Color(
                split.getOrNull(0)?.toIntOrNull() ?: 0,
                split.getOrNull(1)?.toIntOrNull() ?: 0,
                split.getOrNull(2)?.toIntOrNull() ?: 0,
                split.getOrNull(3)?.toFloatOrNull() ?: 0f
            )
        }

        private fun getMultiString(args: Array<out String>?): String {
            return buildString {
                val list = args?.toList() ?: return ""
                list.subList(1, list.size).forEach { append("$it ") }
            }
        }
    }

    private class SetupSet(val builder: MarkerCommand): TabExecutor {
        override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>?): MutableList<String> {
            return buildList {
                when (args?.size ?: 0) {
                    0, 1 -> addAll(listOf("build", "cancel", "map", "toggleable", "default_hidden", "label", "id"))
                    2 -> when (args?.getOrNull(0)) {
                        "map" -> addAll(MarkerManager.getAllMaps())
                        "toggleable", "default_hidden" -> addAll(listOf("true", "false"))
                        "id" -> add("<word>")
                        "label" -> add("<phrase>")
                    }
                    else -> when (args?.getOrNull(0)) {
                        "label" -> add("<phrase>")
                    }
                }
            }.toMutableList()
        }

        override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
            val name = sender.name
            val value = args?.getOrNull(1) ?: ""
            when (args?.getOrNull(0)?.lowercase()) {
                "build" -> builder.buildSet(sender, name)
                "cancel" -> builder.cancel(sender, name, true)
                "map" -> builder.setMarkerArgument(sender, name, MarkerArg.MAP, getMultiString(args), "map $value", true)
                "toggleable" -> builder.setMarkerArgument(sender, name, MarkerArg.TOGGLEABLE, value, "toggleable $value", true)
                "default_hidden" -> builder.setMarkerArgument(sender, name, MarkerArg.DEFAULT_HIDDEN, value, "default hidden $value", true)
                "label" -> builder.setMarkerArgument(sender, name, MarkerArg.LABEL, getMultiString(args), "label ${getMultiString(args)}", true)
                "id" -> builder.setMarkerArgument(sender, name, MarkerArg.ID, value, "id $label", true)
                else -> builder.sendStatusInfo(sender, name, true)
            }
            return true
        }

        private fun getMultiString(args: Array<out String>?): String {
            return buildString {
                val list = args?.toList() ?: return ""
                list.subList(1, list.size).forEach { append("$it ") }
            }
        }
    }

    init {
        PluginManager.getCommand("bmarker")?.setExecutor(this)
        PluginManager.getCommand("bmarker")?.tabCompleter = this

        val setupCMD = Setup(this)
        PluginManager.getCommand("bmarker-setup")?.setExecutor(setupCMD)
        PluginManager.getCommand("bmarker-setup")?.tabCompleter = setupCMD

        val setupSetCMD = SetupSet(this)
        PluginManager.getCommand("bmarker-setup-set")?.setExecutor(setupSetCMD)
        PluginManager.getCommand("bmarker-setup-set")?.tabCompleter = setupSetCMD
    }
}