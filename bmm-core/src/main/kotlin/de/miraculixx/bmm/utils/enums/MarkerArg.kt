package de.miraculixx.bmm.utils.enums

enum class MarkerArg(val isRequired: Boolean, val isList: Boolean = false, val excludeTemplate: Boolean = false) {
    ID(true),
    MARKER_SET(true),
    POSITION(true, excludeTemplate = true),
    LABEL(true),
    ICON(false),
    ANCHOR(false),
    MAX_DISTANCE(false),
    MIN_DISTANCE(false),
    ADD_POSITION(true, true),
    ADD_EDGE(true, true),
    DETAIL(false),
    LINK(false),
    NEW_TAB(false),
    DEPTH_TEST(false),
    LINE_WIDTH(false),
    LINE_COLOR(false),
    FILL_COLOR(false),
    HEIGHT(true),
    MAX_HEIGHT(true),
    POINTS(true),
    X_RADIUS(true),
    Z_RADIUS(true),
    LISTED(false),
    TELEPORTER(false),

    // Marker-Set exclusives
    MAP(true),
    TOGGLEABLE(false),
    DEFAULT_HIDDEN(false),
    LISTING_POSITION(false, excludeTemplate = true),
    ;
}