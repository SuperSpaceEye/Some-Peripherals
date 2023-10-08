package net.spaceeye.someperipherals

import net.spaceeye.someperipherals.config.*

object SomePeripheralsConfig {
    lateinit var server_config_holder: ConfigBuilder
    lateinit var client_config_holder: ConfigBuilder
    lateinit var common_config_holder: ConfigBuilder


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
                var is_enabled: Boolean by CBool(true, "disables functionality of the block")

                var vector_rotation_enabled: Boolean by CBool(true)

                var max_raycast_iterations: Int by CInt(-1, "set to num <=0 for no limit")
                var max_yaw_angle: Double by CDouble(Math.PI / 2, "angle in radians")
                var max_pitch_angle: Double by CDouble(Math.PI / 2, "angle in radians")
                var entity_check_radius: Int by CInt(8, "should be a positive integer")


                var check_for_entities: Boolean by CBool(true, "Includes VS ships if VS exists")

                var return_abs_pos: Boolean by CBool(true)
                var return_distance: Boolean by CBool(true)
                var return_block_id: Boolean by CBool(true)
                var return_ship_id: Boolean by CBool(true, "only if VS is installed")

                var return_entity_type_descriptionId: Boolean by CBool(true)

                var do_position_caching: Boolean by CBool(true)
                var max_cached_positions: Int by CInt(1000)
                var save_cache_for_ticks: Int by CInt(20)
            }
        }
    }
}