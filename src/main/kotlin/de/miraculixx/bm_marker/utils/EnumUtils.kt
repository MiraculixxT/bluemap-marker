package de.miraculixx.bm_marker.utils

inline fun <reified T : Enum<T>> enumOf(type: String?): T? {
    if (type == null) return null
    return try {
        java.lang.Enum.valueOf(T::class.java, type)
    } catch (e: IllegalArgumentException) {
        null
    }
}