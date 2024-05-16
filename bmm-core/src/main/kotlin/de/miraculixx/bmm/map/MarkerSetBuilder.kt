package de.miraculixx.bmm.map

import de.bluecolored.bluemap.api.markers.MarkerSet
import de.miraculixx.bmm.map.data.Box
import de.miraculixx.bmm.map.interfaces.Builder
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.enums.MarkerType

class MarkerSetBuilder(
    private val args: MutableMap<MarkerArg, Box> = mutableMapOf(),
    private val blueMapSet: MarkerSet = MarkerSet("<unset>")
) : Builder {
    override var page = 0

    fun apply(): MarkerSet {
        return blueMapSet.apply {
            args[MarkerArg.LABEL]?.getString()?.let { label = it }
            args[MarkerArg.TOGGLEABLE]?.getBoolean()?.let { isToggleable = it }
            args[MarkerArg.DEFAULT_HIDDEN]?.getBoolean()?.let { isDefaultHidden = it }
            args[MarkerArg.LISTING_POSITION]?.getInt()?.let { sorting = it }
        }
    }

    /*
    Getter and setters
     */
    override fun getType(): MarkerType {
        return MarkerType.MARKER_SET
    }

    override fun getArgs() = args

    override fun setArg(arg: MarkerArg, value: Box) {
        args[arg] = value
    }

    companion object {
        fun createSet(args: MutableMap<MarkerArg, Box>): MarkerSet {
            return MarkerSetBuilder(args).apply()
        }

        fun editSet(set: MarkerSet, changedArgs: MutableMap<MarkerArg, Box>) {
            MarkerSetBuilder(changedArgs, set).apply()
        }
    }
}