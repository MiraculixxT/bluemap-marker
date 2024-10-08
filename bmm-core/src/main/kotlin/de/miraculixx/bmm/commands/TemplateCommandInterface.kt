package de.miraculixx.bmm.commands

import de.miraculixx.bmm.map.MarkerBuilder
import de.miraculixx.bmm.map.MarkerManager
import de.miraculixx.bmm.map.MarkerManager.builder
import de.miraculixx.bmm.map.data.Box
import de.miraculixx.bmm.map.data.MarkerTemplateEntry
import de.miraculixx.bmm.map.data.TemplateSet
import de.miraculixx.bmm.utils.data.templateCommandPrefix
import de.miraculixx.bmm.utils.enums.MarkerArg
import de.miraculixx.bmm.utils.enums.MarkerType
import de.miraculixx.bmm.utils.locale
import de.miraculixx.mcommons.extensions.enumOf
import de.miraculixx.mcommons.extensions.soundDisable
import de.miraculixx.mcommons.extensions.soundEnable
import de.miraculixx.mcommons.text.msg
import de.miraculixx.mcommons.text.plus
import de.miraculixx.mcommons.text.prefix
import net.kyori.adventure.audience.Audience
import kotlin.jvm.optionals.getOrNull

interface TemplateCommandInterface : MarkerBuilderInstance {

    /**
     * Create a new template set.
     * This should be combined with creating a new command.
     */
    fun Audience.createNewSet(name: String, needsPermission: Boolean): TemplateSet? {
        if (MarkerManager.templateSets.containsKey(name)) {
            sendMessage(prefix + locale.msg("command.idAlreadyExist", listOf(name)))
            return null
        }

        val maps = MarkerManager.blueMapAPI?.maps?.map { it.id }?.toMutableSet() ?: mutableSetOf()
        val set = TemplateSet(name, needPermission = needsPermission, maps = maps)
        MarkerManager.blueMapAPI?.let { set.load(it) }
        MarkerManager.templateSets[name] = set
        sendMessage(prefix + locale.msg("command.template.create", listOf(name)))
        return set
    }

    /**
     * Completely delete a template set.
     * Including all sets on all maps and all markers inside.
     */
    fun Audience.deleteSet(name: String, confirm: Boolean): Boolean {
        if (!confirm) {
            sendMessage(prefix + locale.msg("command.template.confirmRemoval", listOf("/$templateCommandPrefix delete $name true")))
            return false
        }
        val set = MarkerManager.templateSets.remove(name)
        if (set == null) {
            sendMessage(prefix + locale.msg("command.notValidSet", listOf("template", name)))
            return false
        }

        set.remove()
        sendMessage(prefix + locale.msg("command.template.delete", listOf(name)))
        return true
    }

    /**
     * Change attributes of the template set.
     * This will apply the changes to all sets on all maps.
     */
    fun Audience.setSetArg(set: TemplateSet, arg: MarkerArg, value: Box) {
        set.templateSet[arg] = value
        set.maps.forEach { map ->
            MarkerManager.blueMapMaps[map]?.get(set.markerSetID)?.getEditor()?.apply {
                setArg(arg, value)
            }?.apply()
        }

        sendMessage(prefix + locale.msg("command.template.setArg", listOf(value.stringify())))
    }

    /**
     * Add a new marker template to the template set.
     * This allows adding new entries under this template.
     */
    fun Audience.addMarkerTemplate(id: String, set: TemplateSet, stringType: String) {
        val type = enumOf<MarkerType>(stringType.uppercase())
        if (type == null) {
            sendMessage(prefix + locale.msg("command.notValidMarker", listOf(stringType)))
            return
        }

        builder[id] = MarkerBuilder(type, templateSet = set)
        sendStatusInfo(this, id, isConsole = id == consoleName)
    }

    fun Audience.editMarkerTemplate(id: String, templateID: String, set: TemplateSet) {
        val bMarker = set.templateMarker[templateID]
        if (bMarker == null) {
            sendMessage(prefix + locale.msg("command.notValidMarker", listOf(templateID)))
            return
        }
        builder[id] = MarkerBuilder(bMarker.type, bMarker.attributes, isEdit = true, templateSet = set)
        sendStatusInfo(this, id, isConsole = id == consoleName)
    }

    /**
     * Remove a marker template from the template set.
     * This will remove all entries under this template.
     */
    fun Audience.removeMarkerTemplate(set: TemplateSet, templateMarkerID: String) {
        val placedMarkers = set.playerMarkers.filter { it.value.templateName == templateMarkerID }
        placedMarkers.forEach { (uuid, data) -> // go through each player marker
            set.playerMarkers.remove(uuid)
            set.blueMapSets.forEach { (_, bSet) -> // go through each map to remove player marker
                bSet.remove(data.id)
            }
        }
        set.templateMarker.remove(templateMarkerID) // remove template
        sendMessage(prefix + locale.msg("command.template.removeMarkerTemplate", listOf(templateMarkerID)))
    }

    /**
     * Add a new map to the template set.
     * This will allow placing markers in the world associated with this map.
     */
    fun Audience.addMap(set: TemplateSet, mapID: String) {
        if (set.maps.contains(mapID)) {
            sendMessage(prefix + locale.msg("command.template.mapAlreadyAdded", listOf(mapID)))
            return
        }
        val map = MarkerManager.blueMapAPI?.getMap(mapID)?.getOrNull()
        if (map == null) {
            sendMessage(prefix + locale.msg("command.mapNotFound", listOf(mapID)))
            return
        }

        set.addMap(mapID, map)
        sendMessage(prefix + locale.msg("command.template.addMap", listOf(mapID)))
    }

    fun Audience.removeMap(set: TemplateSet, mapID: String) {
        if (!set.maps.contains(mapID)) {
            sendMessage(prefix + locale.msg("command.template.mapNotAdded", listOf(mapID)))
            return
        }
        val map = MarkerManager.blueMapAPI?.getMap(mapID)?.getOrNull()
        if (map == null) {
            sendMessage(prefix + locale.msg("command.mapNotFound", listOf(mapID)))
            return
        }

        set.removeMap(mapID, map)
        sendMessage(prefix + locale.msg("command.template.removeMap", listOf(mapID)))
    }

    /**
     * Add a new entry to the template set and place it into the world
     */
    fun Audience.placeMarker(entry: MarkerTemplateEntry, set: TemplateSet, bypass: Boolean, world: Any) {
        val templateMarker = set.templateMarker[entry.templateName]
        if (templateMarker == null) {
            sendMessage(prefix + locale.msg("command.template.noValidTemplate", listOf(entry.templateName)))
            return
        }
        if (!bypass && set.playerMarkers.count { it.value.playerName == entry.playerName } >= set.maxMarkerPerPlayer) {
            sendMessage(prefix + locale.msg("command.maxMarkers", listOf(set.name)))
            return
        }
        val maps = MarkerManager.blueMapAPI?.getWorld(world)?.getOrNull()?.maps?.filter { set.maps.contains(it.id) }?.map { it.id }
        if (maps.isNullOrEmpty()) {
            sendMessage(prefix + locale.msg("command.template.invalidWorld"))
            return
        }

        // Place marker into set and map
        entry.placedMaps.addAll(maps)
        set.playerMarkers[entry.id] = entry
        set.placeMarker(entry, templateMarker)
        soundEnable()
        sendMessage(prefix + locale.msg("command.template.place", listOf(entry.templateName)))
    }

    /**
     * Remove a marker from the world and the template set
     */
    fun Audience.unplaceMarker(set: TemplateSet, markerID: String, bypass: Boolean, playerName: String) {
        val entry = set.playerMarkers[markerID]
        if (entry == null) {
            sendMessage(prefix + locale.msg("command.notValidMarker", listOf(markerID)))
            return
        }
        if (!bypass && entry.playerName != playerName) {
            sendMessage(prefix + locale.msg("command.notYourMarker"))
            return
        }

        // Clean up
        set.unplaceMarker(entry)
        set.playerMarkers.remove(entry.id)
        soundDisable()
        sendMessage(prefix + locale.msg("command.template.unplace", listOf(entry.templateName)))
    }
}