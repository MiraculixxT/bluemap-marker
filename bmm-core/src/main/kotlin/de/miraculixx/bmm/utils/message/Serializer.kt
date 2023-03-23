package de.miraculixx.bmm.utils.message

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.bluecolored.bluemap.api.gson.MarkerGson
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.flattener.ComponentFlattener
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

val plainSerializer = PlainTextComponentSerializer.builder().flattener(ComponentFlattener.textOnly()).build()
val miniMessages = MiniMessage.miniMessage()
val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}
val gson: Gson = MarkerGson.addAdapters(GsonBuilder()).setPrettyPrinting().create()