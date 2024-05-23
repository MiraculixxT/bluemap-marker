package de.miraculixx.bmm.commands

import de.miraculixx.bmm.map.MarkerManager.builder
import de.miraculixx.bmm.map.MarkerManager.builderSet
import de.miraculixx.bmm.map.interfaces.Builder
import de.miraculixx.bmm.utils.data.mainCommandPrefix
import de.miraculixx.bmm.utils.data.setupCommandPrefix
import de.miraculixx.bmm.utils.data.setupSetCommandPrefix
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.locale
import de.miraculixx.bmm.utils.msgBuild
import de.miraculixx.bmm.utils.msgCancel
import de.miraculixx.bmm.utils.msgNotSet
import de.miraculixx.mcommons.text.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

interface MarkerBuilderInstance {

    fun sendStatusInfo(sender: Audience, id: String, isMarkerSet: Boolean = false, updateMessage: Component? = null) {
        val builder = getBuilder(sender, id, isMarkerSet) ?: return
        val type = builder.getType()
        val appliedArgs = builder.getArgs()
        val nothingSet = cmp(locale.msgNotSet(), italic = true)
        val dash = cmp("- ")
        val midDash = cmp(" ≫ ", NamedTextColor.DARK_GRAY)
        val cmd = if (isMarkerSet) "/$setupSetCommandPrefix" else "/$setupCommandPrefix"
        val hoverAddition = cmp("\n\n" + locale.msgString("event.clickToAdd"), cMark)
        if (updateMessage != null) builder.lastEditMessage = updateMessage

        val pages = type.args.filter { builder.templateSet == null || !it.excludeTemplate }.chunked(7)
        val currentPage = pages.getOrElse(builder.page) { pages.first() }

        // Send header
        sender.sendMessage(cmp("\n\n\n\n\n\n\n\n\n"))
        sender.sendMessage(builder.lastEditMessage)
        sender.sendMessage(
            cmp(" \n") + cmp("                       ", cHighlight, strikethrough = true) + cmp("[", cHighlight) +
                    (if (builder.page > 0) cmp(" ← ", cSuccess).addCommand("$cmd page previous").addHover(cmp("Previous Page")) else cmp(" ← ", cError)) +
                    cmp("${builder.page + 1}", cHighlight, true) + cmp("/") + cmp("${pages.size}", cHighlight, true) +
                    (if (builder.page < pages.size - 1) cmp(" → ", cSuccess).addCommand("$cmd page next").addHover(cmp("Next Page")) else cmp(" → ", cError)) +
                    cmp("]", cHighlight) + cmp("                       ", cHighlight, strikethrough = true)
        )

        // Send visible arguments
        currentPage.forEach { arg ->
            // List values displayed in a different way than single values
            if (arg.isList) {
                val list = when (arg) {
                    MarkerArg.ADD_POSITION -> appliedArgs[arg]?.getVector3dList() ?: emptyList()
                    MarkerArg.ADD_EDGE -> appliedArgs[arg]?.getVector2dList() ?: emptyList()
                    else -> emptyList()
                }
                val isSet = list.isNotEmpty()
                val color = if (!isSet) cError else cSuccess
                // - ARG >> [3 Values] [+] [-]
                // - ARG >> Not Set
                val inputText = if (isSet) {
                    cmp("[${list.size} Values]", cMark) +
                            cmp(" [+]", cSuccess).addSuggest("$cmd ${arg.name.lowercase()} ").addHover(cmp("Add a new value") + hoverAddition) +
                            cmp(" [-]", cError).addCommand("$cmd ${arg.name.lowercase()} remove-last").addHover(cmp("Remove the last value"))
                } else nothingSet.addSuggest("$cmd ${arg.name.lowercase()} ").addHover(cmp("Add a new value") + hoverAddition)
                sender.sendMessage(dash + cmp(locale.msgString("arg.${arg.name}"), color) + midDash + inputText)
                return@forEach
            }

            val value = appliedArgs[arg]?.stringify()
            val color = if (value == null) {
                if (arg.isRequired) cError else NamedTextColor.GRAY
            } else cSuccess
            val message = cmp(locale.msgString("arg.${arg.name}"), color) + midDash + if (value != null) cmp("$value", cMark) else nothingSet
            if (arg == MarkerArg.ID && builder.isEdit)
                sender.sendMessage(dash + message.addHover(cmp("The ID cannot be changed after creation!", cError)))
            else sender.sendMessage(
                dash + message
                    .addSuggest("$cmd ${arg.name.lowercase()} ")
                    .addHover(cmp(locale.msgString("arg-desc.${arg.name}")) + hoverAddition)
            )
        }

        // Fill up space if needed
        if (currentPage.size < 7) {
            for (i in 0 until 7 - currentPage.size) {
                sender.sendMessage(emptyComponent())
            }
        }

        // Send footer
        sender.sendMessage(
            cmp("                 ", cHighlight, strikethrough = true) + cmp("[ ", cHighlight) +
                    cmp(locale.msgBuild().uppercase(), cSuccess, true, strikethrough = false).addCommand("$cmd build").addHover(cmp(locale.msgString("event.buildHover"))) +
                    cmp(" | ") +
                    cmp(locale.msgCancel().uppercase(), cError, bold = true, strikethrough = false).addCommand("$cmd cancel").addHover(cmp(locale.msgString("event.cancelHover"))) +
                    cmp(" ]", cHighlight) + cmp("                 ", cHighlight, strikethrough = true)
        )
    }

    fun getBuilder(sender: Audience, id: String, isSet: Boolean = false): Builder? {
        return if (isSet) {
            builderSet.getOrElse(id) {
                noBuilder(sender, true)
                return null
            }
        } else {
            builder.getOrElse(id) {
                noBuilder(sender, true)
                return null
            }
        }
    }

    fun noBuilder(sender: Audience, isSet: Boolean = false) {
        val addition = if (isSet) "set-" else ""
        sender.sendMessage(
            prefix +
                    cmp("You have no current marker$addition setups. Start one with ", cError) +
                    cmp("/$mainCommandPrefix ${addition}create", cError, underlined = true).addSuggest("/$mainCommandPrefix ${addition}create ").addHover(cmp("Start a marker$addition setup (click)"))
        )
    }
}