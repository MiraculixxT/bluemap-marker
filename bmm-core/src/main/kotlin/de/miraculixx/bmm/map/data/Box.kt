package de.miraculixx.bmm.map.data

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector2i
import com.flowpowered.math.vector.Vector3d
import de.bluecolored.bluemap.api.math.Color
import kotlinx.serialization.Serializable

@Suppress("UNCHECKED_CAST")
@JvmInline
@Serializable
value class Box<T>(val value: T) {
    fun getString(): String {
        return if (value is Color) colorToString()
        else value as? String ?: value.toString()
    }

    private fun colorToString(): String {
        val color = getColor() ?: return "Invalid Color"
        return "R:${color.red}, G:${color.green}, B:${color.blue}, Alpha:${color.alpha}"
    }

    fun getInt() = value as? Int
    fun getFloat() = value as? Float
    fun getDouble() = value as? Double
    fun getBoolean() = value as? Boolean
    fun getVector3d() = value as? Vector3d
    fun getVector2i() = value as? Vector2i
    fun getVector2dList() = value as? List<Vector2d>
    fun getVector3dList() = value as? List<Vector3d>
    fun getColor() = value as? Color
}
