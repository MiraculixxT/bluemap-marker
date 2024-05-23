package de.miraculixx.bmm.utils

import de.miraculixx.mcommons.serializer.LocaleSerializer
import de.miraculixx.mcommons.text.msgString
import kotlinx.serialization.Serializable
import java.io.File
import java.util.*

@Serializable
data class Settings(
    var language: @Serializable(with = LocaleSerializer::class) Locale = Locale.ENGLISH,
    var maxUserSets: Int = 1,
    var maxUserMarker: Int = 5
)

var settings = Settings()

lateinit var sourceFolder: File


// Translations
val locale = Locale.ENGLISH
fun Locale.msgCancel() = msgString("common.cancel")
fun Locale.msgBuild() = msgString("common.build")
fun Locale.msgOr() = msgString("common.or")
fun Locale.msgUse() = msgString("common.use")
fun Locale.msgNotSet() = msgString("common.notSet")
