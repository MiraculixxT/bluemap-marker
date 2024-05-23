package de.miraculixx.bmm.map.interfaces

import de.miraculixx.bmm.map.data.Box
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.enums.MarkerType
import de.miraculixx.mcommons.text.cmp
import de.miraculixx.mcommons.text.emptyComponent
import net.kyori.adventure.text.Component

interface Builder {
    var page: Int
    val isEdit: Boolean
    var lastEditMessage: Component

    fun getType(): MarkerType
    fun getArgs(): Map<MarkerArg, Box>
    fun setArg(arg: MarkerArg, value: Box)
}