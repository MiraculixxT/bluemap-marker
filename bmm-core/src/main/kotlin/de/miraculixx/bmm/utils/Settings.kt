package de.miraculixx.bmm.utils

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class Settings(
    var language: String = "en_US",
    var allowUserMarkers: Boolean = false,
)

val settings = Settings()

lateinit var sourceFolder: File
