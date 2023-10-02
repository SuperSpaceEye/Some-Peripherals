package net.spaceeye.someperipherals

import com.github.imifou.jsonschema.module.addon.annotation.JsonSchema

object SomePeripheralsConfig {
    @JvmField
    val CLIENT = Client()
    @JvmField
    val SERVER = Server()

    class Client

    class Server {
        val COMMON = Common()

        class Common {
            val RAYCASTER_SETTINGS = RaycasterSettings()

            class RaycasterSettings {
                @JsonSchema(description = "disables functionality of the block")
                var is_enabled = true
                @JsonSchema
                var vector_rotation_enabled = true

                @JsonSchema(description = "set to num <=0 for no limit")
                var max_raycast_iterations = -1
                @JsonSchema(description = "angle in radians")
                var max_yaw_angle = Math.PI / 2
                @JsonSchema(description = "angle in radians")
                var max_pitch_angle = Math.PI / 2
                @JsonSchema(description = "should be a positive integer divisible by two or it will FUCKING CRASH, probably")
                var entity_check_radius = 8

                @JsonSchema(description = "Includes VS ships if VS exists")
                var check_for_entities: Boolean = true

                var return_abs_pos = true
                var return_distance = true
                var return_block_id = true
                @JsonSchema(description = "only with VS existing")
                var return_ship_id = true

                @JsonSchema(description = "")
                var return_entity_type_descriptionId = true


                var do_position_caching = true
                var max_cached_positions= 1000
                var save_cache_for_ticks = 20

                var debug_offset = 0.0
            }
        }
    }
}