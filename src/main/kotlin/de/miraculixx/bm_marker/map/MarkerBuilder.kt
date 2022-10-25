package de.miraculixx.bm_marker.map

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector2i
import com.flowpowered.math.vector.Vector3d
import de.bluecolored.bluemap.api.markers.*
import de.bluecolored.bluemap.api.math.Line
import de.bluecolored.bluemap.api.math.Shape
import de.miraculixx.bm_marker.map.data.ArgumentValue
import de.miraculixx.bm_marker.map.interfaces.Builder
import de.miraculixx.bm_marker.utils.enums.MarkerArg
import de.miraculixx.bm_marker.utils.enums.MarkerType


class MarkerBuilder(private val type: MarkerType): Builder {
    private val args: MutableMap<MarkerArg, ArgumentValue> = mutableMapOf()
    private val vector3dList: MutableList<Vector3d> = mutableListOf()
    private val vector2dList: MutableList<Vector2d> = mutableListOf()

    fun buildMarker(): Marker? {
        return when (type) {
            MarkerType.POI -> POIMarker.toBuilder().apply {
                applyBasics()

                args[MarkerArg.ICON]?.getString()?.let { icon(it, args[MarkerArg.ANCHOR]?.getVector2d() ?: Vector2i.ZERO) }
                args[MarkerArg.MAX_DISTANCE]?.getDouble()?.let { maxDistance(it) }
                args[MarkerArg.MIN_DISTANCE]?.getDouble()?.let { minDistance(it) }
            }.build()

            MarkerType.LINE -> LineMarker.builder().apply {
                applyBasics()
                if (vector3dList.isEmpty()) return null
                line(Line(vector3dList))

                args[MarkerArg.DETAIL]?.getString()?.let { detail(it) }
                args[MarkerArg.LINK]?.getString()?.let { link(it, args[MarkerArg.NEW_TAB]?.getBoolean() ?: true) }
                args[MarkerArg.DEPTH_TEST]?.getBoolean()?.let { depthTestEnabled(it) }
                args[MarkerArg.LINE_WIDTH]?.getInt()?.let { lineWidth(it) }
                args[MarkerArg.LINE_COLOR]?.getColor()?.let { lineColor(it) }
                args[MarkerArg.MAX_DISTANCE]?.getDouble()?.let { maxDistance(it) }
                args[MarkerArg.MIN_DISTANCE]?.getDouble()?.let { minDistance(it) }
            }.build()

            MarkerType.SHAPE -> ShapeMarker.builder().apply {
                applyBasics()
                if (vector2dList.isEmpty()) return null
                shape(Shape(vector2dList), args[MarkerArg.HEIGHT]?.getFloat() ?: return null)

                args[MarkerArg.DETAIL]?.getString()?.let { detail(it) }
                args[MarkerArg.LINK]?.getString()?.let { link(it, args[MarkerArg.NEW_TAB]?.getBoolean() ?: true) }
                args[MarkerArg.DEPTH_TEST]?.getBoolean()?.let { depthTestEnabled(it) }
                args[MarkerArg.LINE_WIDTH]?.getInt()?.let { lineWidth(it) }
                args[MarkerArg.LINE_COLOR]?.getColor()?.let { lineColor(it) }
                args[MarkerArg.FILL_COLOR]?.getColor()?.let { fillColor(it) }
                args[MarkerArg.MAX_DISTANCE]?.getDouble()?.let { maxDistance(it) }
                args[MarkerArg.MIN_DISTANCE]?.getDouble()?.let { minDistance(it) }
            }.build()

            MarkerType.EXTRUDE -> ExtrudeMarker.builder().apply {
                applyBasics()
                if (vector2dList.isEmpty()) return null
                shape(Shape(vector2dList), args[MarkerArg.HEIGHT]?.getFloat() ?: return null, args[MarkerArg.MAX_HEIGHT]?.getFloat() ?: return null)

                args[MarkerArg.DETAIL]?.getString()?.let { detail(it) }
                args[MarkerArg.LINK]?.getString()?.let { link(it, args[MarkerArg.NEW_TAB]?.getBoolean() ?: true) }
                args[MarkerArg.DEPTH_TEST]?.getBoolean()?.let { depthTestEnabled(it) }
                args[MarkerArg.LINE_WIDTH]?.getInt()?.let { lineWidth(it) }
                args[MarkerArg.LINE_COLOR]?.getColor()?.let { lineColor(it) }
                args[MarkerArg.FILL_COLOR]?.getColor()?.let { fillColor(it) }
                args[MarkerArg.MAX_DISTANCE]?.getDouble()?.let { maxDistance(it) }
                args[MarkerArg.MIN_DISTANCE]?.getDouble()?.let { minDistance(it) }
            }.build()

            else -> return null
        }
    }

    private fun <T : Marker, B : Marker.Builder<T, B>> Marker.Builder<T, B>.applyBasics(): Boolean {
        val label = args[MarkerArg.LABEL]?.getString() ?: return false
        label(label)
        args[MarkerArg.POSITION]?.getVector3d()?.let { position(it) }
        return true
    }

    /*
    Getter and setter
     */
    override fun getType(): MarkerType {
        return type
    }

    override fun getArgs(): Map<MarkerArg, ArgumentValue> {
        return args
    }

    override fun setArg(arg: MarkerArg, value: ArgumentValue) {
        args[arg] = value
    }

    override fun getVec2List(): MutableList<Vector2d> {
        return vector2dList
    }

    override fun getVec3List(): MutableList<Vector3d> {
        return vector3dList
    }
}