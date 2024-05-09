package de.miraculixx.bmm.api

import de.miraculixx.bmm.utils.message.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.net.HttpURLConnection
import java.net.URI

object APIConnector {
    var isOutdated = false
    var outdatedMessage = emptyComponent()

    fun checkVersion(currentVersion: Int): Boolean {
        val version = try {
            val url = URI("https://api.mutils.de/public/version").toURL()
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "GET"
            con.setRequestProperty("User-Agent", "MUtils-API-1.1")
            con.setRequestProperty("Service", "BMM")
            con.doInput = true
            con.doOutput = true
            con.connect()
            json.decodeFromString<Version>(con.inputStream.readBytes().decodeToString())
        } catch (e: Exception) {
            null
        }
        if (version == null) {
            consoleAudience.sendMessage(prefix + cmp("Could not check current version! Proceed at your own risk", cError))
            return true
        }
        outdatedMessage = cmp("Latest Version: ") + cmp(version.latest.toString(), cSuccess) + cmp(" - Installed Version: ") + cmp(currentVersion.toString(), cError)
        if (currentVersion < version.last) {
            consoleAudience.sendMessage(prefix + cmp("You are running a too outdated version of BMM! An update is required due to security reasons or internal changes.", cError))
            consoleAudience.sendMessage(prefix + outdatedMessage)
            isOutdated = true
            return false
        }
        if (currentVersion < version.latest) {
            consoleAudience.sendMessage(prefix + cmp("You are running an outdated version of BMM!"))
            consoleAudience.sendMessage(prefix + outdatedMessage)
            isOutdated = true
        }
        if (currentVersion > version.latest) {
            consoleAudience.sendMessage(prefix + cmp("You are running a beta version. Bugs may appear!"))
        }
        return true
    }

    @Serializable
    private data class Version(val latest: Int, val last: Int)
}