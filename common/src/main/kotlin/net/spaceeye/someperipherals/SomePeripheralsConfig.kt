package net.spaceeye.someperipherals

import net.spaceeye.someperipherals.config.*

object SomePeripheralsConfig {
    lateinit var server_config_holder: AbstractConfigBuilder
    lateinit var client_config_holder: AbstractConfigBuilder
    lateinit var common_config_holder: AbstractConfigBuilder


    val SERVER = Server()
    val CLIENT = Client()
    val COMMON = Common()

    class Client: ConfigSubDirectory()
    class Common: ConfigSubDirectory()

    class Server: ConfigSubDirectory() {
        val COMMON = Common()

        class Common: ConfigSubDirectory() {
            val RAYCASTER_SETTINGS = RaycasterSettings()

            class RaycasterSettings: ConfigSubDirectory() {
                var is_enabled: Boolean by CBool(true, "Disables functionality of the block")

                var vector_rotation_enabled: Boolean by CBool(true, "If false, only euler rotation is allowed")

                var max_raycast_distance: Int by CInt(-1, "Maximum amount of blocks ray can travel. Set to num <=0 for no limit")
                var max_yaw_angle: Double by CDouble(Math.PI / 2, "Only for euler rotation. Angle in radians", Pair(0.0, Double.MAX_VALUE))
                var max_pitch_angle: Double by CDouble(Math.PI / 2, "Only for euler rotation. angle in radians", Pair(0.0, Double.MAX_VALUE))
                var entity_check_radius: Int by CInt(8, "Will check for intersections with entities every N blocks traveled in N radius", Pair(1, Int.MAX_VALUE))

                var check_for_intersection_with_entities: Boolean by CBool(true, "Includes Valkyrien Skies ships if VS is installed")

                var return_abs_pos: Boolean by CBool(true)
                var return_hit_pos: Boolean by CBool(true, "Returns hit position of ray relative to world origin")
                var return_distance: Boolean by CBool(true, "Distance is from ray origin to hit position")
                var return_block_type: Boolean by CBool(true)

                var return_ship_id: Boolean by CBool(true, "Only if Valkyrien Skies is installed and ray hit block on ship")
                val return_shipyard_hit_pos: Boolean by CBool(true, "Only if Valkyrien Skies is installed. Returns hit position of ray relative to shipyard origin")

                var return_entity_type: Boolean by CBool(true)

                var do_position_caching: Boolean by CBool(false, "IT'S BROKEN RN DON'T USE. If true, raycaster will cache traveled blocks for some time")
                var max_cached_positions: Int by CInt(1000, "IT'S BROKEN RN DON'T USE", Pair(1, Int.MAX_VALUE))
                var save_cache_for_N_ticks: Int by CInt(20, "IT'S BROKEN RN DON'T USE. Will clear cache after N tick passed", Pair(1, Int.MAX_VALUE))
            }
        }
    }
}