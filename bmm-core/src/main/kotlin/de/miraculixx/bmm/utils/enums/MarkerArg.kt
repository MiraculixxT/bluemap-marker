package de.miraculixx.bmm.utils.enums

enum class MarkerArg(val isRequired: Boolean) {
    ID(true),
    MARKER_SET(true),
    POSITION(true),
    LABEL(true),
    ICON(false),
    ANCHOR(false),
    MAX_DISTANCE(false),
    MIN_DISTANCE(false),
    ADD_POSITION(true),
    ADD_EDGE(true),
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

    // Marker-Set exclusives
    MAP(true),
    TOGGLEABLE(false),
    DEFAULT_HIDDEN(false),
}