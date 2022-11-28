package de.miraculixx.bmm.utils.message

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.format.NamedTextColor

val prefix = cmp("BMM", cHighlight) + cmp(" >> ", NamedTextColor.DARK_GRAY)
lateinit var consoleAudience: Audience