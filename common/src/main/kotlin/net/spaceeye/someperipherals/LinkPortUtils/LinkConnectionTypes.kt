package net.spaceeye.someperipherals.LinkPortUtils

import net.minecraft.world.entity.Entity
import net.spaceeye.someperipherals.raycasting.RaycastReturn
import net.spaceeye.someperipherals.util.getNow_ms

abstract class LinkUpdate() {
    var timestamp = getNow_ms()
}

abstract class LinkRequest() {}
abstract class LinkResponse() {}

open class Server_EntityPhysUpdate(var data: MutableMap<String, Any>, var entity: Entity): LinkUpdate()
class Server_RangeGogglesPhysUpdate(data: MutableMap<String, Any>, entity: Entity): Server_EntityPhysUpdate(data, entity)

class LinkRaycastRequest(var distance: Double, var euler_mode: Boolean, var do_cache:Boolean, var var1:Double, var var2: Double, var var3: Double): LinkRequest() {}
class LinkRaycastResponse(var result: RaycastReturn): LinkResponse()

class LinkBatchRaycastRequest(
    var distance: Double, var euler_mode: Boolean, var do_cache:Boolean,
    var data: Array<Array<Double>>, var start: Long, var timeout: Long, var do_terminate: Boolean=false): LinkRequest() {}
class LinkBatchRaycastResponse(var results: MutableList<RaycastReturn>, var is_done:Boolean=false): LinkResponse()