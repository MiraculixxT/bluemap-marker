package de.miraculixx.bmm.map.interfaces

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector3d
import de.miraculixx.bmm.map.data.ArgumentValue
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.enums.MarkerType

interface Builder {
    fun getType(): MarkerType
    fun getArgs(): Map<MarkerArg, ArgumentValue>
    fun setArg(arg: MarkerArg, value: ArgumentValue)
    fun getVec3List(): MutableList<Vector3d>
    fun getVec2List(): MutableList<Vector2d>
}