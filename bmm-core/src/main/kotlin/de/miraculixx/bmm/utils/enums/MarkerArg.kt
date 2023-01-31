package de.miraculixx.bmm.utils.enums

enum class MarkerArg(val description: String, val isRequired: Boolean) {
    ID("Internal ID to access the marker(-set)", true),
    MARKER_SET("Target marker-set where the marker\nshould be stored (provides world)", true),
    POSITION("Position where the marker will be \nplaced. (NOT the world)", true),
    LABEL("Displayed label in the marker menu", true),
    ICON("Image URL for displayed image on\ngiven position (POI exclusive)", false),
    ANCHOR("Offset for displayed image/html code (in pixel)", false),
    MAX_DISTANCE("Max distance to the camera at\nwhich the marker is shown", false),
    MIN_DISTANCE("Min distance to the camera at\nwhich the marker is shown", false),
    ADD_POSITION("Add more positions to a marker\nto define his path (Line exclusive)", true),
    ADD_EDGE("Add more edges to a marker\nto define his shape", true),
    DETAIL("Description that is shown on marker click", false),
    LINK("A redirect link on marker click", false),
    NEW_TAB("If the redirect link is opened\nin a new tab or replace BlueMap tab", false),
    DEPTH_TEST("If false the marker will always\nrender above everything. Otherwise\nit will respect blocking layers", false),
    LINE_WIDTH("The width of the line/outline (in pixel)", false),
    LINE_COLOR("Color of the line/outline (RGBA)\n- https://htmlcolorcodes.com -", false),
    FILL_COLOR("Color of the drawn field (RGBA)\n- https://htmlcolorcodes.com -", false),
    HEIGHT("The minimal height of the marker field.\nDefinition height for shape markers", true),
    MAX_HEIGHT("The maximal height of the marker field.\nSet to the same Y height/position\nfor a flat shape", true),
    POINTS("The amount of points used to render\nthe circle/ellipse. More points\ncreate cleaner borders but to many\npoints affect performance!", true),
    X_RADIUS("The ellipse radius to the +-x direction", true),
    Z_RADIUS("The ellipse radius to the +-z direction", true),

    // Marker-Set exclusives
    MAP("Target BlueMap Map", true),
    TOGGLEABLE("If this is true, the marker-set\ncan be enabled or disabled in the menu", false),
    DEFAULT_HIDDEN("If this is true, the marker-set\nwill be hidden by default and can be\nenabled by the user. If 'TOGGELABLE' is\nfalse, this is the forced state", false),
}