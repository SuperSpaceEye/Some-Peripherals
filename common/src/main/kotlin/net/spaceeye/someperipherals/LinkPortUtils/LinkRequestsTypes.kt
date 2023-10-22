// The one doing a request should remove previous response first (if it exists)
// The one executing request should remove it after completing it.

package net.spaceeye.someperipherals.LinkPortUtils

import net.minecraft.world.entity.Entity
import net.spaceeye.someperipherals.raycasting.RaycastReturn

abstract class LinkRequest
abstract class LinkResponse

class LinkStatusRequest : LinkRequest()
class LinkStatusResponse(var data: MutableMap<String, Any>, var entity: Entity): LinkResponse()

class LinkRaycastRequest(var distance: Double, var euler_mode: Boolean, var do_cache:Boolean, var var1:Double, var var2: Double, var var3: Double): LinkRequest() {}
class LinkRaycastResponse(var result: RaycastReturn): LinkResponse()

class LinkBatchRaycastRequest(
    var distance: Double, var euler_mode: Boolean, var do_cache:Boolean,
    var data: Array<Array<Double>>, var start: Long, var timeout: Long, var do_terminate: Boolean=false): LinkRequest()
class LinkBatchRaycastResponse(var results: MutableList<RaycastReturn>, var is_done:Boolean=false): LinkResponse()