package net.spaceeye.someperipherals.LinkPortUtils

import net.minecraft.world.entity.Entity
import net.spaceeye.someperipherals.raycasting.RaycastReturn
import java.util.*

abstract class LinkUpdate() {
    var timestamp = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time.time
}

abstract class LinkRequest() {}
abstract class LinkResponse() {}

open class Server_EntityPhysUpdate(var data: MutableMap<String, Any>, var entity: Entity): LinkUpdate()
class Server_RangeGogglesPhysUpdate(data: MutableMap<String, Any>, entity: Entity): Server_EntityPhysUpdate(data, entity)

class LinkRaycastRequest(var distance: Double, var euler_mode: Boolean, var do_cache:Boolean, var var1:Double, var var2: Double, var var3: Double): LinkRequest() {}
class LinkRaycastResponse(var result: RaycastReturn): LinkResponse()