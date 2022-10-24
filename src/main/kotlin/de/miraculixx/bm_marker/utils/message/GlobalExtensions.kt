package de.miraculixx.bm_marker.utils.message

import de.bluecolored.bluemap.api.math.Color

fun Color.stringify(): String {
    return "(R:$red G:$blue B:$blue Opacity:$alpha)"
}