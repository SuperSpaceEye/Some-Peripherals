package net.spaceeye.someperipherals.utils.configToMap

import net.spaceeye.someperipherals.SomePeripheralsConfig

fun makeRaycastingConfigInfo(): MutableMap<String, Any> {
    val rc = SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS
    val rcc = SomePeripheralsConfig.SERVER.RAYCASTING_SETTINGS
    return mutableMapOf(
        Pair("max_raycast_time_ms", rcc.max_raycast_time_ms),
        Pair("allow_raycasting_for_entities_only", rcc.allow_raycasting_for_entities_only),

        Pair("max_raycast_distance", rc.max_raycast_distance),
        Pair("entity_check_radius", rc.entity_check_radius),

        Pair("max_raycast_no_worldcheking_distance", rc.max_raycast_no_worldcheking_distance),
        Pair("entity_check_radius_no_worldchecking", rc.entity_check_radius_no_worldchecking),

        Pair("check_for_intersection_with_entities", rc.check_for_intersection_with_entities),

        Pair("return_abs_pos", rc.return_abs_pos),
        Pair("return_hit_pos", rc.return_hit_pos),
        Pair("return_distance", rc.return_distance),
        Pair("return_block_type", rc.return_block_type),

        Pair("return_ship_id", rc.return_ship_id),
        Pair("return_shipyard_hit_pos", rc.return_shipyard_hit_pos),

        Pair("return_entity_type", rc.return_entity_type),

        Pair("do_position_caching", rc.do_position_caching),
        Pair("max_cached_positions", rc.max_cached_positions),
        Pair("save_cache_for_N_ticks", rc.save_cache_for_N_ticks),
    )
}