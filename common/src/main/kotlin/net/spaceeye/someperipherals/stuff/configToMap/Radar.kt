package net.spaceeye.someperipherals.stuff.configToMap

import net.spaceeye.someperipherals.SomePeripheralsConfig

fun makeRadarConfigInfo(): MutableMap<String, Any> {
    val rc = SomePeripheralsConfig.SERVER.RADAR_SETTINGS

    return mutableMapOf(
        Pair("max_entity_search_radius", rc.max_entity_search_radius),
        Pair("max_ship_search_radius", rc.max_ship_search_radius)
    )
}