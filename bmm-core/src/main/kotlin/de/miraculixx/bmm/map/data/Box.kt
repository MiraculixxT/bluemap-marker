package de.miraculixx.bmm.map.data

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector2i
import com.flowpowered.math.vector.Vector3d
import de.bluecolored.bluemap.api.math.Color
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed class Box {
    @Serializable data class BoxString(val value: String) : Box() { override fun stringify() = value }
    @Serializable data class BoxInt(val value: Int) : Box() { override fun stringify() = value.toString() }
    @Serializable data class BoxFloat(val value: Float) : Box() { override fun stringify() = value.toString() }
    @Serializable data class BoxDouble(val value: Double) : Box() { override fun stringify() = value.toString() }
    @Serializable data class BoxBoolean(val value: Boolean) : Box() { override fun stringify() = value.toString() }
    @Serializable data class BoxVector3d(val value: @Contextual Vector3d) : Box() { override fun stringify() = value.toString() }
    @Serializable data class BoxVector2i(val value: @Contextual Vector2i) : Box() { override fun stringify() = value.toString() }
    @Serializable data class BoxVector2dList(val value: MutableList<@Contextual Vector2d>) : Box() { override fun stringify() = value.toString() }
    @Serializable data class BoxVector3dList(val value: MutableList<@Contextual Vector3d>) : Box() { override fun stringify() = value.toString() }
    @Serializable data class BoxColor(val value: @Contextual Color) : Box() { override fun stringify() = "(R:${value.red}, G:${value.green}, B:${value.blue}, Alpha:${value.alpha})" }

    fun getString() = (this as? BoxString)?.value
    fun getInt() = (this as? BoxInt)?.value
    fun getFloat() = (this as? BoxFloat)?.value
    fun getDouble() = (this as? BoxDouble)?.value
    fun getBoolean() = (this as? BoxBoolean)?.value
    fun getVector3d() = (this as? BoxVector3d)?.value
    fun getVector2i() = (this as? BoxVector2i)?.value
    fun getVector2dList() = (this as? BoxVector2dList)?.value
    fun getVector3dList() = (this as? BoxVector3dList)?.value
    fun getColor() = (this as? BoxColor)?.value

    abstract fun stringify(): String
}