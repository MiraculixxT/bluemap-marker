package de.miraculixx.bm_marker.utils.message

import net.kyori.adventure.text.flattener.ComponentFlattener
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

val plainSerializer = PlainTextComponentSerializer.builder().flattener(ComponentFlattener.textOnly()).build()
val miniMessages = MiniMessage.miniMessage()