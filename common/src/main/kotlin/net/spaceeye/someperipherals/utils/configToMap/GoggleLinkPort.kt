package net.spaceeye.someperipherals.utils.configToMap

import net.spaceeye.someperipherals.SomePeripheralsConfig

fun makeGoggleLinkPortConfigInfoBase(): MutableMap<String, Any> {
    val lps = SomePeripheralsConfig.SERVER.LINK_PORT_SETTINGS
    return mutableMapOf(
        Pair("max_connection_timeout_time_ticks", lps.max_connection_timeout_time_ticks)
    )
}

fun makeGoggleLinkPortConfigInfoRange(): MutableMap<String, Any> {
    val data = makeRaycastingConfigInfo()
    val first = makeGoggleLinkPortConfigInfoBase()

    val rgc = SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS

    data["max_allowed_waiting_time"] = rgc.max_allowed_raycast_waiting_time_ms
    data["max_connection_timeout_time"] = SomePeripheralsConfig.SERVER.LINK_PORT_SETTINGS.max_connection_timeout_time_ticks

    return data.apply {
        first.forEach { (key, value) ->
            merge(key, value) { currentValue, addedValue ->
                "$currentValue, $addedValue" // just concatenate... no duplicates-check..
            }
        }
    }
}