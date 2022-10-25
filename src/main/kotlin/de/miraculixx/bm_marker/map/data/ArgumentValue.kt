package de.miraculixx.bm_marker.map.data

import com.flowpowered.math.vector.Vector2i
import com.flowpowered.math.vector.Vector3d
import de.bluecolored.bluemap.api.math.Color
import de.miraculixx.bm_marker.utils.message.stringify

data class ArgumentValue(private val value: Any) {
    fun getString(): String {
        return if (value is Color) value.stringify()
        else value as? String ?: value.toString()
    }

    fun getInt(): Int? { return value as? Int }
    fun getFloat(): Float? { return value as? Float }
    fun getDouble(): Double? { return value as? Double }
    fun getBoolean(): Boolean? { return value as? Boolean }
    fun getVector3d(): Vector3d? { return value as? Vector3d }
    fun getVector2d(): Vector2i? { return value as? Vector2i }
    fun getColor(): Color? { return value as? Color }
}