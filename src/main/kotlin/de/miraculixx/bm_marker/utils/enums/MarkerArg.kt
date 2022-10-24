package de.miraculixx.bm_marker.utils.enums

enum class MarkerArg(val description: String, val isRequired: Boolean) {
    ID("Internal ID to access the marker", true),
    WORLD("Target world/dimension", true),
    POSITION("Source position (start point)", true),
    LABEL("Displayed label on the marker menu", true),
    ICON("Image URL for displayed image on\ngiven position (POI exclusive)", false),
    ANCHOR("Offset for displayed image/html code (in pixel)", false),
    MAX_DISTANCE("Max distance to the camera at\nwhich the marker is shown", false),
    MIN_DISTANCE("Min distance to the camera at\nwhich the marker is shown", false),
    ADD_DIRECTION("Add more directions to a marker\nto define his path (Line exclusive)", true),
    ADD_EDGE("Add more edges to a marker\nto define his shape", true),
    DETAIL("Description that is shown on marker click", false),
    LINK("A redirect link on marker click", false),
    NEW_TAB("If the redirect link is opened\nin a new tab or replace BlueMap tab", false),
    DEPTH_TEST("If false the marker will always\nrender above everything. Otherwise\nit will respect blocking layers", false),
    LINE_WIDTH("The width of the line/outline (in pixel)", false),
    LINE_COLOR("Color of the line/outline (RGBA)\n- https://htmlcolorcodes.com -", false),
    FILL_COLOR("Color of the drawn field (RGBA)\n- https://htmlcolorcodes.com -", false),
    HEIGHT("The minimal height of the marker field.\nDefinition height for shape markers", true),
    MAX_HEIGHT("The maximal height of the marker field\n(Extrude exclusive)", true)
}