package de.miraculixx.bmm.api

import de.miraculixx.bmm.utils.message.cmp
import de.miraculixx.bmm.utils.message.consoleAudience
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.reflect.*
import java.lang.reflect.Type
import java.net.URL

object WebClient {
    val client = HttpClient(CIO)

    suspend inline fun get(destination: String, headers: Map<String, String> = emptyMap(), body: String? = null, post: Boolean = false): String {
        return try {
            if (post) proceedPost(destination, headers, body).bodyAsText()
            else proceedGet(destination, headers, body).bodyAsText()
        } catch (e: Exception) {
            consoleAudience.sendMessage(cmp("Failed to resolve $destination"))
            return ""
        }
    }

    suspend fun getFile(destination: String, headers: Map<String, String> = emptyMap(), body: String? = null): ByteArray? {
        return try {
            proceedGet(destination, headers, body).body() as ByteArray
        } catch (e: Exception) {
            consoleAudience.sendMessage(cmp("Failed to resolve $destination (file)"))
            return null
        }
    }

    suspend inline fun proceedGet(
        destination: String,
        headers: Map<String, String> = emptyMap(),
        body: String?,
    ): HttpResponse = client.get(URL(destination)) {
        header("User-Agent", "BlueMap-MarkerManager")
        headers.forEach { (key, value) -> header(key, value) }
        contentType(ContentType.Application.Json)
        body?.let { setBody(it) }
    }

    suspend inline fun proceedPost(
        destination: String,
        headers: Map<String, String> = emptyMap(),
        body: String?,
    ): HttpResponse = client.post(URL(destination)) {
        header("User-Agent", "BlueMap-MarkerManager")
        headers.forEach { (key, value) -> header(key, value) }
        contentType(ContentType.Application.Json)
        body?.let { setBody(it) }
    }
}