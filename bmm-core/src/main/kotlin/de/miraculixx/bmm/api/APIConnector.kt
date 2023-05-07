package de.miraculixx.bmm.api

import de.miraculixx.bmm.utils.message.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.security.MessageDigest


object APIConnector {
    var isOutdated = false

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    suspend fun checkVersion(loader: Loader, version: String) {
        val currentFile = File(APIConnector::class.java.protectionDomain.codeSource.location.toURI())
        val sha = MessageDigest.getInstance("SHA-512").digest(currentFile.readBytes()).toHexString()
        consoleAudience.sendMessage(prefix + cmp("SHA - $sha"))

        //Get current version
        val responseCurrent = WebClient.get("https://api.modrinth.com/v2/version_file/$sha?algorithm=sha512")
        val responseLatest = WebClient.get(
            "https://api.modrinth.com/v2/version_file/$sha/update?algorithm=sha512",
            body = json.encodeToString(ModrinthRequest(setOf(loader.name.lowercase()), setOf(version))),
            post = true
        )
        try {
            val currentVersion = json.decodeFromString<ModrinthVersion>(responseCurrent)
            val latestVersion = json.decodeFromString<ModrinthVersion>(responseLatest)
            if (latestVersion.version_number != currentVersion.version_number) {
                //Outdated
                isOutdated = true
                consoleAudience.sendMessage(prefix + cmp("You are running an outdated version of BlueMap-Marker!", cError))
                consoleAudience.sendMessage(prefix + cmp("Latest Version: ") + cmp(latestVersion.version_number, cSuccess) + cmp(" - Installed Version: ") + cmp(currentVersion.version_number, cError))
                consoleAudience.sendMessage(prefix + cmp("Update -> https://modrinth.com/mod/bmarker"))
            } else consoleAudience.sendMessage(prefix + cmp("Running on the latest version", cSuccess))
        } catch (_: Exception) {
            consoleAudience.sendMessage(prefix + cmp("Failed to test for latest version", cError))
        }
    }
}