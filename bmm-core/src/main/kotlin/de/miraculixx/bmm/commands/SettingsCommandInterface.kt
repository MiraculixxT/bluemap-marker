package de.miraculixx.bmm.commands

import de.miraculixx.bmm.utils.locale
import de.miraculixx.mcommons.text.msg
import de.miraculixx.mcommons.text.plus
import de.miraculixx.mcommons.text.prefix
import net.kyori.adventure.audience.Audience

interface SettingsCommandInterface {

    fun sendCurrentInfo(sender: Audience, value: String) {
        sender.sendMessage(prefix + locale.msg("command.settings.current", listOf(value)))
    }

    fun sendChangedInfo(sender: Audience, value: String) {
        sender.sendMessage(prefix + locale.msg("command.settings.changed", listOf(value)))
    }
}