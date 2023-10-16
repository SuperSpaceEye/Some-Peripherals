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
        val RAYCASTER_SETTINGS = RaycasterSettings()
        val LINK_PORT_SETTINGS = LinkPortSettings()
        val GOGGLE_SETTINGS = GogglesSettings()

        class RaycasterSettings: ConfigSubDirectory() {
            var is_enabled: Boolean by CBool(true, "Disables functionality of the block")

            var vector_rotation_enabled: Boolean by CBool(true, "If false, only euler rotation is allowed")

            var max_raycast_distance: Int by CInt(-1, "Maximum amount of blocks ray can travel. Set to num <=0 for no limit")
            var max_yaw_angle: Double by CDouble(Math.PI / 2, "Only for euler rotation. Angle in radians", Pair(0.0, Double.MAX_VALUE))
            var max_pitch_angle: Double by CDouble(Math.PI / 2, "Only for euler rotation. angle in radians", Pair(0.0, Double.MAX_VALUE))
            var entity_check_radius: Int by CInt(16, "Will check for intersections with entities every N blocks traveled in N radius", Pair(1, Int.MAX_VALUE))

            var check_for_intersection_with_entities: Boolean by CBool(true, "Includes Valkyrien Skies ships if VS is installed")

            var return_abs_pos: Boolean by CBool(true)
            var return_hit_pos: Boolean by CBool(true, "Returns hit position of ray relative to world origin")
            var return_distance: Boolean by CBool(true, "Distance is from ray origin to hit position")
            var return_block_type: Boolean by CBool(true)

            var return_ship_id: Boolean by CBool(true, "If ray hit block on ship", SomePeripherals.has_vs)
            val return_shipyard_hit_pos: Boolean by CBool(true, "Returns hit position of ray relative to shipyard origin", SomePeripherals.has_vs)

            var return_entity_type: Boolean by CBool(true)

            var do_position_caching: Boolean by CBool(true, "If true, raycaster will cache traveled blocks for some time")
            var max_cached_positions: Int by CInt(1000, "", Pair(1, Int.MAX_VALUE))
            var save_cache_for_N_ticks: Int by CInt(20, "Will clear raycaster cache after N MC ticks passed", Pair(1, Int.MAX_VALUE))
        }

        class LinkPortSettings: ConfigSubDirectory() {
            var max_connection_timeout_time: Double by CDouble(2.0, "If time between now and previous update is bigger than N, then the connection is treated as terminated. In seconds", Pair(0.0, Double.MAX_VALUE))
        }

        class GogglesSettings: ConfigSubDirectory() {
            val ALLOWED_GOGGLE_DATA = AllowedGoggleData()

            val RANGE_GOGGLES_SETTINGS = RangeGogglesSettings()

            class AllowedGoggleData: ConfigSubDirectory() {
                var pos: Boolean by CBool(true)
                var eye_pos: Boolean by CBool(true)
                var eye_height: Boolean by CBool(true)
                var look_angle: Boolean by CBool(true)
                var dimension: Boolean by CBool(true)
                var entity_type: Boolean by CBool(true)
                var air_supply: Boolean by CBool(true)
                var max_air_supply: Boolean by CBool(true)

                var health: Boolean by CBool(true)
                var max_health: Boolean by CBool(true)
                var armor_value: Boolean by CBool(true)
                var armor_cover_percentage: Boolean by CBool(true)
                var absorption_amount: Boolean by CBool(true)
                var is_baby: Boolean by CBool(true)
                var is_blocking: Boolean by CBool(true)
                var is_sleeping: Boolean by CBool(true)
                var is_fall_flying: Boolean by CBool(true)
                var speed: Boolean by CBool(true)

                var nickname: Boolean by CBool(true)
                var experience_level: Boolean by CBool(true)
                var xp_needed_for_next_level: Boolean by CBool(true)
                var experience_progress: Boolean by CBool(true)
            }

            class RangeGogglesSettings: ConfigSubDirectory() {
                var max_allowed_waiting_time: Double by CDouble(4.0, "In seconds", Pair(0.0, 10.0))
                var thread_awaiting_sleep_time: Long by CLong(100L, "In milliseconds", Pair(0L, Long.MAX_VALUE))
            }
        }
    }
}