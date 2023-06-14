package de.miraculixx.bmm.utils

import kotlinx.serialization.Serializable

@Serializable
data class Settings(var language: String = "en_US")

val settings = Settings()