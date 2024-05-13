package de.miraculixx.bmm.map.data

import de.miraculixx.bmm.utils.enums.MarkerArg
import kotlinx.serialization.Serializable

@Serializable
data class MarkerTemplate(
    val attributes: Set<MarkerArg>
)