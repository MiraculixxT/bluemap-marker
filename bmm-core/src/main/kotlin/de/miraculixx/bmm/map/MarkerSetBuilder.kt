package de.miraculixx.bmm.map

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector3d
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.miraculixx.bmm.map.data.Box
import de.miraculixx.bmm.map.interfaces.Builder
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.enums.MarkerType

class MarkerSetBuilder(
    private val args: MutableMap<MarkerArg, Box<Any>> = mutableMapOf()
) : Builder {
    override var page = 0

    fun buildMarkerSet(): MarkerSet? {
        return MarkerSet.builder().apply {
            label(args[MarkerArg.LABEL]?.getString() ?: return null)
            toggleable(args[MarkerArg.TOGGLEABLE]?.getBoolean() ?: true)
            defaultHidden(args[MarkerArg.DEFAULT_HIDDEN]?.getBoolean() ?: false)
        }.build()
    }

    /*
    Getter and setters
     */
    override fun getType(): MarkerType {
        return MarkerType.MARKER_SET
    }

    override fun getArgs(): Map<MarkerArg, Box<Any>> {
        return args
    }

    override fun setArg(arg: MarkerArg, value: Box<Any>) {
        args[arg] = value
    }

    override fun getVec3List(): MutableList<Vector3d> {
        return mutableListOf()
    }

    override fun getVec2List(): MutableList<Vector2d> {
        return mutableListOf()
    }

    companion object {
        fun ofArguments(args: MutableMap<MarkerArg, Box<Any>>): MarkerSet? {
            return MarkerSetBuilder(args).buildMarkerSet()
        }
    }
}