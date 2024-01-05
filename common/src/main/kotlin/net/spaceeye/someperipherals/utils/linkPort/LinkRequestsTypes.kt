// The one doing a request should remove previous response first (if it exists)
// The one executing request should remove it after completing it.

package net.spaceeye.someperipherals.utils.linkPort

import net.minecraft.world.entity.Entity
import net.spaceeye.someperipherals.utils.raycasting.RaycastReturn

abstract class LinkRequest
abstract class LinkResponse

class LinkStatusRequest : LinkRequest()
class LinkStatusResponse(var data: MutableMap<String, Any>, var entity: Entity): LinkResponse()

class LinkRaycastRequest(var distance: Double, var euler_mode: Boolean, var var1:Double, var var2: Double, var var3: Double, var check_for_blocks_in_world:Boolean, var only_distance: Boolean): LinkRequest()
class LinkRaycastResponse(var result: RaycastReturn): LinkResponse()

class LinkBatchRaycastRequest(var distance: Double, var euler_mode: Boolean, var check_for_blocks_in_world:Boolean, var data: Array<Array<Double>>, var only_distance: Boolean): LinkRequest()
class LinkBatchRaycastResponse(var results: MutableList<RaycastReturn>, var is_done:Boolean=false): LinkResponse()