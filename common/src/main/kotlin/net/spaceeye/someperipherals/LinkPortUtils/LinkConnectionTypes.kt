package net.spaceeye.someperipherals.LinkPortUtils

import net.minecraft.world.entity.Entity
import java.util.*

abstract class LinkUpdate() {
    var timestamp = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time.time
}

open class Server_EntityPhysUpdate(var entity: Entity): LinkUpdate()
class Server_RangeGogglesPhysUpdate(entity: Entity): Server_EntityPhysUpdate(entity)