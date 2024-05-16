package de.miraculixx.bmm.map.interfaces

import de.miraculixx.bmm.map.data.Box
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.enums.MarkerType

interface Builder {
    var page: Int

    fun getType(): MarkerType
    fun getArgs(): Map<MarkerArg, Box>
    fun setArg(arg: MarkerArg, value: Box)
}