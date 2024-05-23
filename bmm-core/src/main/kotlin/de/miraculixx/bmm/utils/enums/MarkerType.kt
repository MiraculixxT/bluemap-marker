package de.miraculixx.bmm.utils.enums

import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector3d
import de.bluecolored.bluemap.api.markers.ExtrudeMarker
import de.bluecolored.bluemap.api.markers.LineMarker
import de.bluecolored.bluemap.api.markers.Marker
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.bluecolored.bluemap.api.markers.POIMarker
import de.bluecolored.bluemap.api.markers.ShapeMarker
import de.bluecolored.bluemap.api.math.Line
import de.bluecolored.bluemap.api.math.Shape

enum class MarkerType(val args: List<MarkerArg>) {
    POI(
        listOf(
            MarkerArg.ID,
            MarkerArg.LABEL,
            MarkerArg.POSITION,
            MarkerArg.ANCHOR,
            MarkerArg.ICON,
            MarkerArg.MIN_DISTANCE,
            MarkerArg.MAX_DISTANCE,
            MarkerArg.LISTED,
            MarkerArg.LISTING_POSITION,
        )
    ),
    LINE(
        listOf(
            MarkerArg.ID,
            MarkerArg.LABEL,
            MarkerArg.ADD_POSITION,
            MarkerArg.DETAIL,
            MarkerArg.LINK,
            MarkerArg.NEW_TAB,
            MarkerArg.DEPTH_TEST,
            MarkerArg.LINE_WIDTH,
            MarkerArg.LINE_COLOR,
            MarkerArg.MIN_DISTANCE,
            MarkerArg.MAX_DISTANCE,
            MarkerArg.LISTED,
            MarkerArg.LISTING_POSITION,
        )
    ),
    SHAPE(
        listOf(
            MarkerArg.ID,
            MarkerArg.LABEL,
            MarkerArg.ADD_EDGE,
            MarkerArg.HEIGHT,
            MarkerArg.DETAIL,
            MarkerArg.LINK,
            MarkerArg.NEW_TAB,
            MarkerArg.DEPTH_TEST,
            MarkerArg.LINE_WIDTH,
            MarkerArg.LINE_COLOR,
            MarkerArg.FILL_COLOR,
            MarkerArg.MIN_DISTANCE,
            MarkerArg.MAX_DISTANCE,
            MarkerArg.LISTED,
            MarkerArg.LISTING_POSITION,
        )
    ),
    EXTRUDE(
        listOf(
            MarkerArg.ID,
            MarkerArg.LABEL,
            MarkerArg.ADD_EDGE,
            MarkerArg.HEIGHT,
            MarkerArg.MAX_HEIGHT,
            MarkerArg.DETAIL,
            MarkerArg.LINK,
            MarkerArg.NEW_TAB,
            MarkerArg.DEPTH_TEST,
            MarkerArg.LINE_WIDTH,
            MarkerArg.LINE_COLOR,
            MarkerArg.FILL_COLOR,
            MarkerArg.MIN_DISTANCE,
            MarkerArg.MAX_DISTANCE,
            MarkerArg.LISTED,
            MarkerArg.LISTING_POSITION,
        )
    ),
    ELLIPSE(
        listOf(
            MarkerArg.ID,
            MarkerArg.LABEL,
            MarkerArg.POSITION,
            MarkerArg.MAX_HEIGHT,
            MarkerArg.X_RADIUS,
            MarkerArg.Z_RADIUS,
            MarkerArg.POINTS,
            MarkerArg.DETAIL,
            MarkerArg.LINK,
            MarkerArg.NEW_TAB,
            MarkerArg.DEPTH_TEST,
            MarkerArg.LINE_WIDTH,
            MarkerArg.LINE_COLOR,
            MarkerArg.FILL_COLOR,
            MarkerArg.MIN_DISTANCE,
            MarkerArg.MAX_DISTANCE,
            MarkerArg.LISTED,
            MarkerArg.LISTING_POSITION,
        )
    ),

    MARKER_SET(
        listOf(
            MarkerArg.ID,
            MarkerArg.LABEL,
            MarkerArg.TOGGLEABLE,
            MarkerArg.DEFAULT_HIDDEN,
            MarkerArg.LISTING_POSITION,
        )
    )
    ;

    fun getEmptyMarker(): Marker {
        val unset = "<unset>"
        return when (this) {
            POI -> POIMarker(unset, Vector3d())
            LINE -> LineMarker(unset, Line(Vector3d(), Vector3d()))
            SHAPE -> ShapeMarker(unset, Shape(Vector2d(), Vector2d(), Vector2d()), 0f)
            EXTRUDE, ELLIPSE -> ExtrudeMarker(unset, Shape(Vector2d(), Vector2d(), Vector2d()), 0f, 0f)
            MARKER_SET -> throw IllegalArgumentException("MarkerSet is not a marker type")
        }
    }
}