package de.miraculixx.bmm.utils.data

import java.util.UUID

data class PlayerData(
    val uuid: UUID?,
    val name: String,
    val permMarkerOther: Boolean,
    val permSetOther: Boolean,
)