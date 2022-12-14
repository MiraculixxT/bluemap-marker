package de.miraculixx.bmm.utils.message

import de.bluecolored.bluemap.api.math.Color

fun Color.stringify(): String {
    return "R:$red G:$green B:$blue Opacity:$alpha"
}

fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}