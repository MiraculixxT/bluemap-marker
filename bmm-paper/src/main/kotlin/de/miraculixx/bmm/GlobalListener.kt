package de.miraculixx.bmm

import de.miraculixx.bmm.api.APIConnector
import de.miraculixx.bmm.utils.message.cMark
import de.miraculixx.bmm.utils.message.cmp
import de.miraculixx.bmm.utils.message.plus
import de.miraculixx.bmm.utils.message.prefix
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object GlobalListener: Listener {

    @EventHandler
    fun playerJoinEvent(it: PlayerJoinEvent) {
        val player = it.player
        if (APIConnector.isOutdated && player.hasPermission("bmm.updater")) {
            player.sendMessage(prefix + cmp("You are running an outdated version of BMM!"))
            player.sendMessage(prefix + APIConnector.outdatedMessage)
            player.sendMessage(prefix + cmp("Click ") + cmp("here", cMark).clickEvent(ClickEvent.openUrl("https://modrinth.com/mod/bmarker")) + cmp(" to install the newest version."))
        }
    }
}