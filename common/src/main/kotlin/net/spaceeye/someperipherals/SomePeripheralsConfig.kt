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
        val RAYCASTING_SETTINGS = RaycastingSettings()
        val RAYCASTER_SETTINGS = RaycasterSettings()
        val LINK_PORT_SETTINGS = LinkPortSettings()
        val RADAR_SETTINGS = RadarSettings()
        val GOGGLE_SETTINGS = GogglesSettings()

        class RaycastingSettings: ConfigSubDirectory() {
            var max_raycast_time_ms: Long by CLong(100L, "Max time before yielding")
        }

        class RaycasterSettings: ConfigSubDirectory() {
            var max_raycast_distance: Int by CInt(-1, "Maximum amount of blocks ray can travel. Set to num <=0 for no limit")
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
            var max_connection_timeout_time_ticks: Long by CLong(5L, "If time between now and previous update is bigger than N, then the connection is treated as terminated. In mc ticks", Pair(0L, Long.MAX_VALUE))
        }

        class RadarSettings: ConfigSubDirectory() {
            class RadarAllowedEntityData: ConfigSubDirectory() {
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
                var xRot: Boolean by CBool(true, "pitch. In degrees.")
                var yHeadRot: Boolean by CBool(true, "yaw. In degrees.")
                var yBodyRot: Boolean by CBool(true, "yaw. In degrees")

                var nickname: Boolean by CBool(true)
                var experience_level: Boolean by CBool(true)
                var xp_needed_for_next_level: Boolean by CBool(true)
                var experience_progress: Boolean by CBool(true)
            }

            val ALLOWED_ENTITY_DATA_SETTINGS = RadarAllowedEntityData()
            val ALLOWED_SHIP_DATA_SETTINGS = AllowedShipData()

            var max_entity_search_radius: Double by CDouble(-1.0, "Max radius of a scan for entities. If <=0 then unlimited.")
            var max_ship_search_radius: Double by CDouble(-1.0, "Max radius of a scan for ships. <=0 for unlimited.", do_show = SomePeripherals.has_vs)
        }

        class AllowedShipData: ConfigSubDirectory() {
            var id: Boolean by CBool(true)
            var pos: Boolean by CBool(true)
            var mass: Boolean by CBool(true)
            var rotation: Boolean by CBool(true)
            var velocity: Boolean by CBool(true)
            var size: Boolean by CBool(true)
            var scale: Boolean by CBool(true)
            var moment_of_inertia_tensor: Boolean by CBool(true)
        }

        class GogglesSettings: ConfigSubDirectory() {
            class GogglesAllowedEntityData: ConfigSubDirectory() {
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
                var xRot: Boolean by CBool(true, "pitch. In degrees.")
                var yHeadRot: Boolean by CBool(true, "yaw. In degrees.")
                var yBodyRot: Boolean by CBool(true, "yaw. In degrees")

                var nickname: Boolean by CBool(true)
                var experience_level: Boolean by CBool(true)
                var xp_needed_for_next_level: Boolean by CBool(true)
                var experience_progress: Boolean by CBool(true)
            }

            val RANGE_GOGGLES_SETTINGS = RangeGogglesSettings()
            val ALLOWED_ENTITY_DATA_SETTINGS = GogglesAllowedEntityData()

            class RangeGogglesSettings: ConfigSubDirectory() {
                var max_allowed_raycast_waiting_time_ms: Long by CLong(4000L, "In milliseconds", Pair(0L, Long.MAX_VALUE))
                var max_batch_raycast_time_ms: Long by CLong(100L, "In milliseconds", Pair(0L, Long.MAX_VALUE))
            }
        }
    }
}