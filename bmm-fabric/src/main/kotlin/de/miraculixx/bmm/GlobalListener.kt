package de.miraculixx.bmm

import de.miraculixx.bmm.api.APIConnector
import de.miraculixx.bmm.utils.message.cMark
import de.miraculixx.bmm.utils.message.cmp
import de.miraculixx.bmm.utils.message.plus
import de.miraculixx.bmm.utils.message.prefix
import net.kyori.adventure.text.event.ClickEvent
import net.silkmc.silk.core.annotations.ExperimentalSilkApi
import net.silkmc.silk.core.event.Events
import net.silkmc.silk.core.event.Player

object GlobalListener {
    @OptIn(ExperimentalSilkApi::class)
    private val onConnect = Events.Player.postLogin.listen { event ->
        val player = event.player
        if (APIConnector.isOutdated && event.player.hasPermissions(3)) {
            player.sendMessage(prefix + cmp("You are running an outdated version of MWeb!"))
            player.sendMessage(prefix + APIConnector.outdatedMessage)
            player.sendMessage(prefix + cmp("Click ") + cmp("here", cMark).clickEvent(ClickEvent.openUrl("https://modrinth.com/mod/mweb")) + cmp(" to install the newest version."))
        }
    }
}