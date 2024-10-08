package de.miraculixx.bmm.map

import com.flowpowered.math.vector.Vector2i
import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.markers.*
import de.bluecolored.bluemap.api.math.Line
import de.bluecolored.bluemap.api.math.Shape
import de.miraculixx.bmm.map.data.Box
import de.miraculixx.bmm.map.data.TemplateSet
import de.miraculixx.bmm.map.interfaces.Builder
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.enums.MarkerType
import de.miraculixx.mcommons.text.emptyComponent
import net.kyori.adventure.text.Component
import java.net.URI
import javax.imageio.ImageIO
import kotlin.jvm.optionals.getOrNull


class MarkerBuilder(
    private val type: MarkerType,
    private val args: MutableMap<MarkerArg, Box> = mutableMapOf(),
    private val blueMapMarker: Marker = type.getEmptyMarker(),
    override val isEdit: Boolean = false,
    override val templateSet: TemplateSet? = null
) : Builder {
    override var page = 0
    override var lastEditMessage: Component = emptyComponent()
    private var missingImportant = false

    fun apply(): Pair<Marker, Boolean> {
        return when (val marker = blueMapMarker) {
            is POIMarker -> marker.apply {
                applyBasics()

                args[MarkerArg.ICON]?.getString()?.let { setIcon(it, getAnchor(args[MarkerArg.ANCHOR], it)) }
            }

            is LineMarker -> marker.apply {
                applyBasics()

                args[MarkerArg.ADD_POSITION]?.getVector3dList()?.let { line = Line(it); centerPosition() } ?: importantArgument()
                args[MarkerArg.DEPTH_TEST]?.getBoolean()?.let { isDepthTestEnabled = it }
                args[MarkerArg.LINE_WIDTH]?.getInt()?.let { lineWidth = it }
                args[MarkerArg.LINE_COLOR]?.getColor()?.let { lineColor = it }
            }

            is ShapeMarker -> marker.apply {
                applyBasics()

                args[MarkerArg.ADD_EDGE]?.getVector2dList()?.let { setShape(Shape(it), args[MarkerArg.HEIGHT]?.getFloat() ?: 50f) } ?: importantArgument()
                args[MarkerArg.DEPTH_TEST]?.getBoolean()?.let { isDepthTestEnabled = it }
                args[MarkerArg.LINE_WIDTH]?.getInt()?.let { lineWidth = it }
                args[MarkerArg.LINE_COLOR]?.getColor()?.let { lineColor = it }
                args[MarkerArg.FILL_COLOR]?.getColor()?.let { fillColor = it }
            }

            is ExtrudeMarker -> marker.apply {
                applyBasics()

                if (this@MarkerBuilder.type == MarkerType.ELLIPSE) {
                    val position = args[MarkerArg.POSITION]?.getVector3d()
                    val x = args[MarkerArg.X_RADIUS]?.getDouble()
                    if (position != null && x != null) {
                        val shape = Shape.createEllipse(position.x, position.z, x, args[MarkerArg.Z_RADIUS]?.getDouble() ?: x, args[MarkerArg.POINTS]?.getInt() ?: 50)
                        setShape(shape, position.y.toFloat(), args[MarkerArg.MAX_HEIGHT]?.getFloat() ?: (position.y.toFloat() + 1f))
                    } else importantArgument()
                } else {
                    args[MarkerArg.ADD_EDGE]?.getVector2dList()?.let {
                        setShape(Shape(it), args[MarkerArg.HEIGHT]?.getFloat() ?: 50f, args[MarkerArg.MAX_HEIGHT]?.getFloat() ?: 51f)
                    } ?: importantArgument()
                }

                args[MarkerArg.DEPTH_TEST]?.getBoolean()?.let { isDepthTestEnabled = it }
                args[MarkerArg.LINE_WIDTH]?.getInt()?.let { lineWidth = it }
                args[MarkerArg.LINE_COLOR]?.getColor()?.let { lineColor = it }
                args[MarkerArg.FILL_COLOR]?.getColor()?.let { fillColor = it }
            }

            else -> blueMapMarker
        } to missingImportant
    }

    private fun Marker.applyBasics() {
        args[MarkerArg.LABEL]?.getString()?.let { label = it } ?: importantArgument()
        args[MarkerArg.POSITION]?.getVector3d()?.let { position = it }
        args[MarkerArg.LISTED]?.getBoolean()?.let { isListed = it }
        args[MarkerArg.LISTING_POSITION]?.getInt()?.let { sorting = it }

        if (this is DetailMarker)
                (args[MarkerArg.DETAIL] ?: args[MarkerArg.LABEL])?.getString()?.let { detail = it }
        if (this is DistanceRangedMarker) {
            args[MarkerArg.MAX_DISTANCE]?.getDouble()?.let { maxDistance = it }
            args[MarkerArg.MIN_DISTANCE]?.getDouble()?.let { minDistance = it }
        }
        if (this is ObjectMarker) {
            args[MarkerArg.LINK]?.getString()?.let { setLink(it, args[MarkerArg.NEW_TAB]?.getBoolean() ?: true) }
        }
    }

    private fun getAnchor(box: Box?, iconPath: String): Vector2i {
        box?.getVector2i()?.let { return it } // Anchor was manually set

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

    private fun importantArgument() {
        missingImportant = true
    }

    override fun getType() = type

    override fun getArgs() = args

    override fun setArg(arg: MarkerArg, value: Box) {
        args[arg] = value
    }

    companion object {
        fun editMarker(marker: Marker, changedArgs: MutableMap<MarkerArg, Box>, type: MarkerType) {
            MarkerBuilder(type, changedArgs, marker).apply()
        }

        fun createMarker(attributes: MutableMap<MarkerArg, Box>, type: MarkerType): Marker {
            return MarkerBuilder(type, attributes).apply().first
        }
    }
}
