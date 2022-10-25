package de.miraculixx.bm_marker.utils.message

import de.bluecolored.bluemap.api.math.Color

fun Color.stringify(): String {
    return "(R:$red G:$blue B:$blue Opacity:$alpha)"
}

fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}