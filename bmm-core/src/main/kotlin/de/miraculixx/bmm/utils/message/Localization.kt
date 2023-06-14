package de.miraculixx.bmm.utils.message

import de.miraculixx.bmm.utils.Config
import net.kyori.adventure.text.Component
import java.io.File
import java.io.InputStream
import java.util.*

private var localization: Config? = null

/**
 * Get a translation for the given key. If no translation were found the key will be returned in red. All inputs will be deserialized with miniMessages
 * @param key Localization Key
 * @param input Input variables. <input-i>
 */
fun msg(key: String, input: List<String> = emptyList()) = miniMessages.deserialize("<!i>" + (localization?.get<String>(key)?.replaceInput(input) ?: "<red>$key"))

/**
 * Get a translation for the given key. If no translation were found the key will be returned instead.
 * @param key Localization Key
 * @param input Input variables. <input-i>
 */
fun msgString(key: String, input: List<String> = emptyList()) = localization?.get<String>(key)?.replaceInput(input) ?: key

/**
 * Get a translation for the given key. If no translation were found the key will be returned in red.
 * @param key Localization Key
 * @param input Input variables. <input-i>
 * @param inline Inline string before every line (useful for listing)
 */
fun msgList(key: String, input: List<String> = emptyList(), inline: String = "<grey>   ") = msgString(key, input).split("<br>").map {
    miniMessages.deserialize("$inline<!i>$it")
}.ifEmpty { listOf(cmp(inline + key, cError)) }

fun getLocal(): Locale {
    return try {
        Locale.forLanguageTag(localization?.name) ?: Locale.ENGLISH
    } catch (_: Exception) {
        Locale.ENGLISH
    }
}

private fun String.replaceInput(input: List<String>): String {
    var msg = this
    input.forEachIndexed { index, s -> msg = msg.replace("<input-${index.plus(1)}>", s) }
    return msg
}

class Localization(private val folder: File, active: String, keys: List<Pair<String, InputStream?>>, private val prefix: Component) {
    private val languages: MutableList<String> = mutableListOf()

    fun getLoadedKeys(): List<String> {
        return languages
    }

    fun setLanguage(key: String): Boolean {
        val file = File("${folder.path}/$key.yml")
        if (!file.exists()) {
            if (debug) consoleAudience.sendMessage(prefix + cmp("LANG - $key file not exist"))
            return false
        }
        val config = Config(file.inputStream(), key, file)
        if (config.get<String>("version") == null) {
            if (debug) consoleAudience.sendMessage(prefix + cmp("LANG - $key file is not a valid language config"))
            return false
        }
        consoleAudience.sendMessage(prefix + cmp("Changed language to ") + cmp(key, cHighlight))
        localization = config
        return true
    }

    private fun checkFiles() {
        languages.clear()
        folder.listFiles()?.forEach {
            if (debug) consoleAudience.sendMessage(prefix + cmp("LANG - Founded language file '${it.name}'! Load it"))
            languages.add(it.nameWithoutExtension)
        }
    }

    init {
        if (!folder.exists()) folder.mkdirs()
        keys.forEach {
            if (debug) consoleAudience.sendMessage(prefix + cmp("LANG - Detect default language '${it.first}' - Corrupted: ${it.second == null}"))
            it.second?.readAllBytes()?.let { bytes -> File("${folder.path}/${it.first}.yml").writeBytes(bytes) }
        }
        checkFiles()
        setLanguage(active)
    }
}
