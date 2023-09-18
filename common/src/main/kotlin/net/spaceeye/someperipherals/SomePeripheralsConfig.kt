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
                @JsonSchema
                val is_enabled = true

                @JsonSchema(description = "set to num <=0 for no limit")
                val max_raycast_iterations = -1
                @JsonSchema(description = "angle in radians")
                val max_yaw_angle = Math.PI / 2
                @JsonSchema(description = "angle in radians")
                val max_pitch_angle = Math.PI / 2

                @JsonSchema(description = "")
                val check_block_model_ray_intersection = true
                @JsonSchema(description = "")
                val check_for_entities: Boolean = true

                @JsonSchema(description = "")
                val return_abs_pos = true
                @JsonSchema(description = "")
                val return_distance = true
                @JsonSchema(description = "")
                val return_block_id = true
            }
        }
    }
}