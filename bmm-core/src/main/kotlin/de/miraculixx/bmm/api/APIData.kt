package de.miraculixx.bmm.api

import kotlinx.serialization.Serializable

@Serializable
data class ModrinthVersion(val name: String, val version_number: String, val changelog: String, val files: List<ModrinthFile>)

/**
 * @param url File URL to download
 * @param filename File name
 * @param size File size in bytes
 */
@Serializable
data class ModrinthFile(val url: String, val filename: String, val size: Long)

@Serializable
data class ModrinthRequest(val loaders: Set<String>, val game_versions: Set<String>)