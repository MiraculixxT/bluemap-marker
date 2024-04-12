package de.miraculixx.bmm.map

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector2i
import com.flowpowered.math.vector.Vector3d
import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.markers.*
import de.bluecolored.bluemap.api.math.Line
import de.bluecolored.bluemap.api.math.Shape
import de.miraculixx.bmm.map.data.ArgumentValue
import de.miraculixx.bmm.map.interfaces.Builder
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.enums.MarkerType
import java.net.URI
import javax.imageio.ImageIO
import kotlin.jvm.optionals.getOrNull


class MarkerBuilder(private val type: MarkerType): Builder {
    private val args: MutableMap<MarkerArg, ArgumentValue> = mutableMapOf()
    private val vector3dList: MutableList<Vector3d> = mutableListOf()
    private val vector2dList: MutableList<Vector2d> = mutableListOf()

	private fun getAnchor(argumentValue: ArgumentValue?, iconPath: String): Vector2i {
        argumentValue?.getVector2i()?.let { return it } // Anchor was manually set

		// Anchor was not set, trying to get it from the image
		val url = runCatching { URI(iconPath).toURL() }.getOrNull()

        return runCatching {
            val bufferedImage = if (url === null) {
                // URL was null, assuming a local path from BlueMap webroot
                val blueMapAPI = BlueMapAPI.getInstance().getOrNull() ?: return Vector2i.ZERO //BlueMap is not loaded, so can't get webroot
                blueMapAPI.webApp.webRoot.resolve(iconPath).toFile().let { ImageIO.read(it) }
            } else {
                ImageIO.read(url)
            } ?: return Vector2i.ZERO // Image could not be ImageIO.read

            Vector2i(bufferedImage.width / 2, bufferedImage.height / 2)
        }.getOrElse { Vector2i.ZERO }
	}

    fun build(): Marker? {
        return when (type) {
            MarkerType.POI -> POIMarker.builder().apply {
                applyBasics()

                args[MarkerArg.DETAIL]?.getString()?.let { detail(it) }
                args[MarkerArg.ICON]?.getString()?.let { icon(it, getAnchor(args[MarkerArg.ANCHOR], it)) }
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

                applyExtrudeData()
            }.build()

            MarkerType.ELLIPSE -> ExtrudeMarker.builder().apply {
                applyBasics()
                val position = args[MarkerArg.POSITION]?.getVector3d() ?: return null
                val shape = Shape.createEllipse(position.x, position.z,
                    args[MarkerArg.X_RADIUS]?.getDouble() ?: return null,
                    args[MarkerArg.Z_RADIUS]?.getDouble() ?: return null,
                    args[MarkerArg.POINTS]?.getInt() ?: return null)
                shape(shape, position.y.toFloat(), args[MarkerArg.MAX_HEIGHT]?.getFloat() ?: return null)

                applyExtrudeData()
            }.build()

            else -> return null
        }
    }

    private fun <T : Marker, B : Marker.Builder<T, B>> Marker.Builder<T, B>.applyBasics(): Boolean {
        val label = args[MarkerArg.LABEL]?.getString() ?: return false
        label(label)
        args[MarkerArg.POSITION]?.getVector3d()?.let { position(it) }
        args[MarkerArg.LISTED]?.getBoolean()?.let { listed(it) }
        return true
    }

    private fun ExtrudeMarker.Builder.applyExtrudeData() {
        args[MarkerArg.DETAIL]?.getString()?.let { detail(it) }
        args[MarkerArg.LINK]?.getString()?.let { link(it, args[MarkerArg.NEW_TAB]?.getBoolean() ?: true) }
        args[MarkerArg.DEPTH_TEST]?.getBoolean()?.let { depthTestEnabled(it) }
        args[MarkerArg.LINE_WIDTH]?.getInt()?.let { lineWidth(it) }
        args[MarkerArg.LINE_COLOR]?.getColor()?.let { lineColor(it) }
        args[MarkerArg.FILL_COLOR]?.getColor()?.let { fillColor(it) }
        args[MarkerArg.MAX_DISTANCE]?.getDouble()?.let { maxDistance(it) }
        args[MarkerArg.MIN_DISTANCE]?.getDouble()?.let { minDistance(it) }
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

    companion object {
        fun of(marker: Marker, type: MarkerType, markerID: String, setID: String): MarkerBuilder? {
            val builder = MarkerBuilder(type)
            builder.args[MarkerArg.LABEL] = ArgumentValue(marker.label)
            builder.args[MarkerArg.POSITION] = ArgumentValue(marker.position)
            builder.args[MarkerArg.ID] = ArgumentValue(markerID)
            builder.args[MarkerArg.MARKER_SET] = ArgumentValue(setID)

            when (type) {
                MarkerType.POI -> {
                    val poi = marker as? POIMarker ?: return null
                    builder.args[MarkerArg.ICON] = ArgumentValue(poi.iconAddress)
                    builder.args[MarkerArg.ANCHOR] = ArgumentValue(poi.anchor)
                    builder.args[MarkerArg.MIN_DISTANCE] = ArgumentValue(poi.minDistance)
                    builder.args[MarkerArg.MAX_DISTANCE] = ArgumentValue(poi.maxDistance)
                }
                MarkerType.LINE -> {
                    val line = marker as? LineMarker ?: return null
                    builder.getVec3List().addAll(line.line.points)
                    builder.args[MarkerArg.DETAIL] = ArgumentValue(line.detail)
                    builder.args[MarkerArg.LINK] = ArgumentValue(line.link.orElse(""))
                    builder.args[MarkerArg.NEW_TAB] = ArgumentValue(line.isNewTab)
                    builder.args[MarkerArg.DEPTH_TEST] = ArgumentValue(line.isDepthTestEnabled)
                    builder.args[MarkerArg.LINE_WIDTH] = ArgumentValue(line.lineWidth)
                    builder.args[MarkerArg.LINE_COLOR] = ArgumentValue(line.lineColor)
                    builder.args[MarkerArg.MIN_DISTANCE] = ArgumentValue(line.minDistance)
                    builder.args[MarkerArg.MAX_DISTANCE] = ArgumentValue(line.maxDistance)
                }
                MarkerType.SHAPE -> {
                    val shape = marker as? ShapeMarker ?: return null
                    builder.getVec2List().addAll(shape.shape.points)
                    builder.args[MarkerArg.DETAIL] = ArgumentValue(shape.detail)
                    builder.args[MarkerArg.LINK] = ArgumentValue(shape.link.orElse(""))
                    builder.args[MarkerArg.NEW_TAB] = ArgumentValue(shape.isNewTab)
                    builder.args[MarkerArg.DEPTH_TEST] = ArgumentValue(shape.isDepthTestEnabled)
                    builder.args[MarkerArg.LINE_WIDTH] = ArgumentValue(shape.lineWidth)
                    builder.args[MarkerArg.LINE_COLOR] = ArgumentValue(shape.lineColor)
                    builder.args[MarkerArg.HEIGHT] = ArgumentValue(shape.shapeY)
                    builder.args[MarkerArg.FILL_COLOR] = ArgumentValue(shape.fillColor)
                    builder.args[MarkerArg.MIN_DISTANCE] = ArgumentValue(shape.minDistance)
                    builder.args[MarkerArg.MAX_DISTANCE] = ArgumentValue(shape.maxDistance)
                }
                MarkerType.EXTRUDE, MarkerType.ELLIPSE -> {
                    val extrude = marker as? ExtrudeMarker ?: return null
                    builder.getVec2List().addAll(extrude.shape.points)
                    builder.args[MarkerArg.DETAIL] = ArgumentValue(extrude.detail)
                    builder.args[MarkerArg.LINK] = ArgumentValue(extrude.link.orElse(""))
                    builder.args[MarkerArg.NEW_TAB] = ArgumentValue(extrude.isNewTab)
                    builder.args[MarkerArg.DEPTH_TEST] = ArgumentValue(extrude.isDepthTestEnabled)
                    builder.args[MarkerArg.LINE_WIDTH] = ArgumentValue(extrude.lineWidth)
                    builder.args[MarkerArg.LINE_COLOR] = ArgumentValue(extrude.lineColor)
                    builder.args[MarkerArg.HEIGHT] = ArgumentValue(extrude.shapeMinY)
                    builder.args[MarkerArg.MAX_HEIGHT] = ArgumentValue(extrude.shapeMaxY)
                    builder.args[MarkerArg.FILL_COLOR] = ArgumentValue(extrude.fillColor)
                    builder.args[MarkerArg.MIN_DISTANCE] = ArgumentValue(extrude.minDistance)
                    builder.args[MarkerArg.MAX_DISTANCE] = ArgumentValue(extrude.maxDistance)
                }

                MarkerType.MARKER_SET -> Unit
            }
            return builder
        }
    }
}
